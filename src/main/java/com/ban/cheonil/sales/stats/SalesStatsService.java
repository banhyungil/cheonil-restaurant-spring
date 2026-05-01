package com.ban.cheonil.sales.stats;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.menu.MenuCategoryRepo;
import com.ban.cheonil.menu.MenuRepo;
import com.ban.cheonil.menu.entity.Menu;
import com.ban.cheonil.menu.entity.MenuCategory;
import com.ban.cheonil.order.OrderMenuRepo;
import com.ban.cheonil.order.OrderRepo;
import com.ban.cheonil.order.dto.OrderMenuExtRes;
import com.ban.cheonil.order.entity.Order;
import com.ban.cheonil.order.entity.OrderStatus;
import com.ban.cheonil.payment.PaymentRepo;
import com.ban.cheonil.payment.entity.PayType;
import com.ban.cheonil.payment.entity.Payment;
import com.ban.cheonil.sales.stats.dto.CategoryPart;
import com.ban.cheonil.sales.stats.dto.DateRangeParams;
import com.ban.cheonil.sales.stats.dto.HourBucket;
import com.ban.cheonil.sales.stats.dto.MenuRank;
import com.ban.cheonil.sales.stats.dto.PayMethodPart;
import com.ban.cheonil.sales.stats.dto.StatsBasicRes;
import com.ban.cheonil.sales.stats.dto.StatsGranularity;
import com.ban.cheonil.sales.stats.dto.StatsMenuRes;
import com.ban.cheonil.sales.stats.dto.StatsStoreParams;
import com.ban.cheonil.sales.stats.dto.StatsStoreRes;
import com.ban.cheonil.sales.stats.dto.StatsTrendParams;
import com.ban.cheonil.sales.stats.dto.StatsTrendRes;
import com.ban.cheonil.sales.stats.dto.StoreCount;
import com.ban.cheonil.sales.stats.dto.StoreMenuPart;
import com.ban.cheonil.sales.stats.dto.StorePayDistribution;
import com.ban.cheonil.sales.stats.dto.StoreSales;
import com.ban.cheonil.sales.stats.dto.StoreUnpaid;
import com.ban.cheonil.sales.stats.dto.TrendPoint;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

/**
 * 통계 서비스 — 주문내역관리 페이지의 통계 탭 4 endpoint 처리.
 *
 * <p>접근 방식: SQL 집계 대신 기간 내 주문 + 결제 + 메뉴 한번에 fetch 후 Java 스트림으로 집계. POS 규모 (월~연 단위에도 ~수만 row)
 * 에선 인메모리 처리가 충분하고 코드도 단순.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesStatsService {

  private final OrderRepo orderRepo;
  private final OrderMenuRepo orderMenuRepo;
  private final PaymentRepo paymentRepo;
  private final StoreRepo storeRepo;
  private final MenuRepo menuRepo;
  private final MenuCategoryRepo menuCategoryRepo;

  /* =========================================================
   * Basic — 시간대 / 점포 TOP 5 / 결제유형 / 메뉴 TOP 5
   * ========================================================= */

  public StatsBasicRes basic(DateRangeParams params) {
    OffsetDateTime[] cur = dayRange(params.from(), params.to());
    OffsetDateTime[] prev = prevDayRange(params.from(), params.to());

    List<Order> orders = orderRepo.findAll(rangeSpec(cur));
    int totalSales = orders.stream().mapToInt(Order::getAmount).sum();
    int totalCount = orders.size();
    int prevSales = orderRepo.sumAmountByOrderAtRange(prev[0], prev[1]);
    long prevCount = orderRepo.count(rangeSpec(prev));

    // hourly 9~20시
    Map<Integer, Integer> hourMap =
        orders.stream()
            .collect(
                Collectors.groupingBy(
                    o -> o.getOrderAt().getHour(), Collectors.summingInt(Order::getAmount)));
    List<HourBucket> hourly =
        IntStream.rangeClosed(9, 20)
            .mapToObj(h -> new HourBucket(h, hourMap.getOrDefault(h, 0)))
            .toList();

    List<StoreSales> storesTop5 = topStores(orders, 5);
    List<PayMethodPart> payParts = aggregatePayParts(orders);
    List<MenuRank> menusTop5 = topMenusByCount(orders, 5);

    return new StatsBasicRes(
        totalSales, prevSales, totalCount, (int) prevCount, hourly, storesTop5, payParts,
        menusTop5);
  }

  /* =========================================================
   * Trend — granularity 별 합계
   * ========================================================= */

  public StatsTrendRes trend(StatsTrendParams params) {
    OffsetDateTime[] cur = dayRange(params.from(), params.to());
    OffsetDateTime[] prev = prevDayRange(params.from(), params.to());

    List<Order> curOrders = orderRepo.findAll(rangeSpec(cur));
    List<Order> prevOrders = orderRepo.findAll(rangeSpec(prev));

    return new StatsTrendRes(
        params.granularity(),
        bucketByGranularity(curOrders, params.granularity()),
        bucketByGranularity(prevOrders, params.granularity()));
  }

  /* =========================================================
   * Menu — TOP 10 / 카테고리별 / 결제별 인기 / 피크타임
   * ========================================================= */

  public StatsMenuRes menu(DateRangeParams params) {
    OffsetDateTime[] cur = dayRange(params.from(), params.to());
    List<Order> orders = orderRepo.findAll(rangeSpec(cur));

    List<MenuRank> menusTop10 = topMenusByCount(orders, 10);
    List<CategoryPart> categoryParts = aggregateCategoryParts(orders);

    // 결제수단별 인기 — orders 를 payType 으로 분류
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    Map<Long, List<Payment>> paymentsByOrder =
        orderSeqs.isEmpty()
            ? Map.of()
            : paymentRepo.findByOrderSeqIn(orderSeqs).stream()
                .collect(Collectors.groupingBy(Payment::getOrderSeq));

    List<Order> cashOrders =
        orders.stream()
            .filter(o -> hasPayType(paymentsByOrder.get(o.getSeq()), PayType.CASH))
            .toList();
    List<Order> cardOrders =
        orders.stream()
            .filter(o -> hasPayType(paymentsByOrder.get(o.getSeq()), PayType.CARD))
            .toList();
    List<Order> peakOrders =
        orders.stream().filter(o -> o.getOrderAt().getHour() == 12).toList();

    return new StatsMenuRes(
        menusTop10,
        categoryParts,
        topMenusByCount(cashOrders, 5),
        topMenusByCount(cardOrders, 5),
        topMenusByCount(peakOrders, 5));
  }

  /* =========================================================
   * Store — 점포별 매출/메뉴 비중/주문 빈도/미수/결제분포
   * ========================================================= */

  public StatsStoreRes store(StatsStoreParams params) {
    OffsetDateTime[] cur = dayRange(params.from(), params.to());
    List<Order> orders = orderRepo.findAll(rangeSpec(cur));

    // 점포별 매출
    List<StoreSales> stores = topStores(orders, Integer.MAX_VALUE);

    // 점포별 주문 건수
    Map<Short, Long> countMap =
        orders.stream().collect(Collectors.groupingBy(Order::getStoreSeq, Collectors.counting()));
    Map<Short, Store> storeEntities = fetchStores(countMap.keySet());
    List<StoreCount> orderCounts =
        countMap.entrySet().stream()
            .sorted(Map.Entry.<Short, Long>comparingByValue().reversed())
            .map(
                e ->
                    new StoreCount(
                        e.getKey(), storeNm(storeEntities, e.getKey()), e.getValue().intValue()))
            .toList();

    // 점포별 미수
    Map<Short, int[]> unpaidAcc =
        orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.PAID)
            .collect(
                Collectors.toMap(
                    Order::getStoreSeq,
                    o -> new int[] {o.getAmount(), 1},
                    (a, b) -> new int[] {a[0] + b[0], a[1] + b[1]}));
    List<StoreUnpaid> unpaidByStore =
        unpaidAcc.entrySet().stream()
            .map(
                e ->
                    new StoreUnpaid(
                        e.getKey(),
                        storeNm(storeEntities, e.getKey()),
                        e.getValue()[0],
                        e.getValue()[1]))
            .toList();

    // 결제 분포 — 점포별 cash / card / unpaid 합계
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    Map<Long, List<Payment>> paymentsByOrder =
        orderSeqs.isEmpty()
            ? Map.of()
            : paymentRepo.findByOrderSeqIn(orderSeqs).stream()
                .collect(Collectors.groupingBy(Payment::getOrderSeq));
    Map<Short, int[]> distAcc = new java.util.HashMap<>(); // [cash, card, unpaid]
    for (Order o : orders) {
      int[] acc = distAcc.computeIfAbsent(o.getStoreSeq(), k -> new int[3]);
      List<Payment> ps = paymentsByOrder.getOrDefault(o.getSeq(), List.of());
      if (ps.isEmpty()) {
        acc[2] += o.getAmount();
      } else {
        for (Payment p : ps) {
          if (p.getPayType() == PayType.CASH) acc[0] += p.getAmount();
          else if (p.getPayType() == PayType.CARD) acc[1] += p.getAmount();
        }
      }
    }
    List<StorePayDistribution> payDistribution =
        distAcc.entrySet().stream()
            .map(
                e ->
                    new StorePayDistribution(
                        e.getKey(),
                        storeNm(storeEntities, e.getKey()),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[2]))
            .toList();

    // 점포별 메뉴 비중 — params.storeSeq() 가 있으면 그 점포만, 없으면 전체 점포
    List<StoreMenuPart> storeMenuParts = computeStoreMenuParts(orders, params.storeSeq());

    return new StatsStoreRes(
        stores, storeMenuParts, orderCounts, unpaidByStore, payDistribution);
  }

  /* =========================================================
   * Helpers
   * ========================================================= */

  private OffsetDateTime[] dayRange(LocalDate from, LocalDate to) {
    return new OffsetDateTime[] {
      from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
      to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
    };
  }

  /** 직전 동일 길이 기간. {@code [from-N, from)}, N = to-from+1. */
  private OffsetDateTime[] prevDayRange(LocalDate from, LocalDate to) {
    int n = (int) (to.toEpochDay() - from.toEpochDay() + 1);
    return new OffsetDateTime[] {
      from.minusDays(n).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
      from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
    };
  }

  private Specification<Order> rangeSpec(OffsetDateTime[] range) {
    return Specification.allOf(
        (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("orderAt"), range[0]),
        (r, q, cb) -> cb.lessThan(r.get("orderAt"), range[1]));
  }

  private Map<Short, Store> fetchStores(Set<Short> storeSeqs) {
    if (storeSeqs.isEmpty()) return Map.of();
    return storeRepo.findAllById(storeSeqs).stream()
        .collect(Collectors.toMap(Store::getSeq, Function.identity()));
  }

  private String storeNm(Map<Short, Store> map, Short seq) {
    Store s = map.get(seq);
    return s != null ? s.getNm() : null;
  }

  private List<StoreSales> topStores(List<Order> orders, int limit) {
    Map<Short, Integer> sumMap =
        orders.stream()
            .collect(
                Collectors.groupingBy(Order::getStoreSeq, Collectors.summingInt(Order::getAmount)));
    Map<Short, Store> storeMap = fetchStores(sumMap.keySet());
    return sumMap.entrySet().stream()
        .sorted(Map.Entry.<Short, Integer>comparingByValue().reversed())
        .limit(limit)
        .map(e -> new StoreSales(e.getKey(), storeNm(storeMap, e.getKey()), e.getValue()))
        .toList();
  }

  private List<PayMethodPart> aggregatePayParts(List<Order> orders) {
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    List<Payment> payments =
        orderSeqs.isEmpty() ? List.of() : paymentRepo.findByOrderSeqIn(orderSeqs);
    int cash =
        payments.stream()
            .filter(p -> p.getPayType() == PayType.CASH)
            .mapToInt(Payment::getAmount)
            .sum();
    int card =
        payments.stream()
            .filter(p -> p.getPayType() == PayType.CARD)
            .mapToInt(Payment::getAmount)
            .sum();
    int unpaid =
        orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.PAID)
            .mapToInt(Order::getAmount)
            .sum();
    int total = cash + card + unpaid;
    return List.of(
        new PayMethodPart("CASH", cash, percent(cash, total)),
        new PayMethodPart("CARD", card, percent(card, total)),
        new PayMethodPart("UNPAID", unpaid, percent(unpaid, total)));
  }

  /** menu count 기준 TOP N. amount 도 함께 집계. */
  private List<MenuRank> topMenusByCount(List<Order> orders, int limit) {
    if (orders.isEmpty()) return List.of();
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    List<OrderMenuExtRes> items = orderMenuRepo.findExtsByOrderSeqs(orderSeqs);

    Map<Short, MenuAcc> accMap = new java.util.HashMap<>();
    for (OrderMenuExtRes om : items) {
      MenuAcc acc = accMap.computeIfAbsent(om.menuSeq(), k -> new MenuAcc(om.menuNm()));
      acc.count += om.cnt();
      acc.amount += om.price() * om.cnt();
    }
    return accMap.values().stream()
        .sorted(Comparator.comparingInt((MenuAcc a) -> a.count).reversed())
        .limit(limit)
        .map(a -> new MenuRank(a.nm, a.count, a.amount))
        .toList();
  }

  private List<CategoryPart> aggregateCategoryParts(List<Order> orders) {
    if (orders.isEmpty()) return List.of();
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    List<OrderMenuExtRes> items = orderMenuRepo.findExtsByOrderSeqs(orderSeqs);
    if (items.isEmpty()) return List.of();

    Set<Short> menuSeqs = items.stream().map(OrderMenuExtRes::menuSeq).collect(Collectors.toSet());
    Map<Short, Menu> menuMap =
        menuRepo.findAllById(menuSeqs).stream()
            .collect(Collectors.toMap(Menu::getSeq, Function.identity()));

    Map<Short, Integer> ctgAmount = new java.util.HashMap<>();
    for (OrderMenuExtRes om : items) {
      Menu m = menuMap.get(om.menuSeq());
      if (m == null) continue;
      ctgAmount.merge(m.getCtgSeq(), om.price() * om.cnt(), Integer::sum);
    }

    Map<Short, MenuCategory> ctgMap =
        menuCategoryRepo.findAllById(ctgAmount.keySet()).stream()
            .collect(Collectors.toMap(MenuCategory::getSeq, Function.identity()));
    int total = ctgAmount.values().stream().mapToInt(Integer::intValue).sum();
    return ctgAmount.entrySet().stream()
        .sorted(Map.Entry.<Short, Integer>comparingByValue().reversed())
        .map(
            e -> {
              MenuCategory c = ctgMap.get(e.getKey());
              return new CategoryPart(
                  c != null ? c.getNm() : null, percent(e.getValue(), total), e.getValue());
            })
        .toList();
  }

  private List<TrendPoint> bucketByGranularity(List<Order> orders, StatsGranularity g) {
    if (orders.isEmpty()) return List.of();
    Map<LocalDate, Integer> bucketSum = new TreeMap<>(); // 정렬 자동
    for (Order o : orders) {
      LocalDate key = bucketKey(o.getOrderAt().toLocalDate(), g);
      bucketSum.merge(key, o.getAmount(), Integer::sum);
    }
    return bucketSum.entrySet().stream()
        .map(e -> new TrendPoint(formatLabel(e.getKey(), g), e.getValue()))
        .toList();
  }

  private LocalDate bucketKey(LocalDate d, StatsGranularity g) {
    return switch (g) {
      case day -> d;
      case week -> d.with(WeekFields.ISO.dayOfWeek(), 1); // Monday
      case month -> d.withDayOfMonth(1);
    };
  }

  private String formatLabel(LocalDate d, StatsGranularity g) {
    return switch (g) {
      case day -> d.getMonthValue() + "/" + d.getDayOfMonth();
      case week -> {
        int weekOfMonth = d.get(WeekFields.ISO.weekOfMonth());
        yield d.getMonthValue() + "월 " + weekOfMonth + "주";
      }
      case month -> String.format(Locale.ROOT, "%02d월", d.getMonthValue());
    };
  }

  private List<StoreMenuPart> computeStoreMenuParts(List<Order> orders, Short storeSeqFilter) {
    List<Order> targetOrders =
        storeSeqFilter == null
            ? orders
            : orders.stream().filter(o -> o.getStoreSeq().equals(storeSeqFilter)).toList();
    if (targetOrders.isEmpty()) return List.of();

    // 점포별 그룹핑
    Map<Short, List<Order>> byStore =
        targetOrders.stream().collect(Collectors.groupingBy(Order::getStoreSeq));

    List<StoreMenuPart> result = new java.util.ArrayList<>();
    for (Map.Entry<Short, List<Order>> e : byStore.entrySet()) {
      Short ss = e.getKey();
      List<Long> orderSeqs = e.getValue().stream().map(Order::getSeq).toList();
      List<OrderMenuExtRes> items = orderMenuRepo.findExtsByOrderSeqs(orderSeqs);
      if (items.isEmpty()) continue;

      Map<Short, MenuAcc> accMap = new java.util.HashMap<>();
      for (OrderMenuExtRes om : items) {
        MenuAcc acc = accMap.computeIfAbsent(om.menuSeq(), k -> new MenuAcc(om.menuNm()));
        acc.count += om.cnt();
      }
      int totalCount = accMap.values().stream().mapToInt(a -> a.count).sum();
      List<StoreMenuPart.Item> top4 =
          accMap.values().stream()
              .sorted(Comparator.comparingInt((MenuAcc a) -> a.count).reversed())
              .limit(4)
              .map(a -> new StoreMenuPart.Item(a.nm, a.count, percent(a.count, totalCount)))
              .toList();
      int top4Count = top4.stream().mapToInt(StoreMenuPart.Item::count).sum();
      int etcCount = totalCount - top4Count;
      result.add(new StoreMenuPart(ss, top4, etcCount));
    }
    return result;
  }

  private boolean hasPayType(List<Payment> payments, PayType type) {
    if (payments == null || payments.isEmpty()) return false;
    return payments.stream().anyMatch(p -> p.getPayType() == type);
  }

  private double percent(int part, int total) {
    if (total == 0) return 0d;
    return Math.round((part * 10000.0) / total) / 100.0; // 소수점 2자리
  }

  /** menu count + amount 누적용 mutable holder. */
  private static final class MenuAcc {
    final String nm;
    int count = 0;
    int amount = 0;

    MenuAcc(String nm) {
      this.nm = nm;
    }
  }
}
