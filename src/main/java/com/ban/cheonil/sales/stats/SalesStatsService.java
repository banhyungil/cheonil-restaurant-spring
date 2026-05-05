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
import com.ban.cheonil.setting.SettingService;
import com.ban.cheonil.setting.entity.OperatingHours;
import com.ban.cheonil.sales.stats.dto.CategoryPart;
import com.ban.cheonil.sales.stats.dto.DateRangeParams;
import com.ban.cheonil.sales.stats.dto.HourBucket;
import com.ban.cheonil.sales.stats.dto.MenuRank;
import com.ban.cheonil.sales.stats.dto.PayMethodPart;
import com.ban.cheonil.sales.stats.dto.StatsBasicRes;
import com.ban.cheonil.sales.stats.dto.StatsGranularity;
import com.ban.cheonil.sales.stats.dto.StatsHourMenuStack;
import com.ban.cheonil.sales.stats.dto.StatsHourMenuStack.HourMenuStack;
import com.ban.cheonil.sales.stats.dto.StatsMenuRes;
import com.ban.cheonil.sales.stats.dto.StatsStoreRes;
import com.ban.cheonil.sales.stats.dto.StatsTrendParams;
import com.ban.cheonil.sales.stats.dto.StatsTrendRes;
import com.ban.cheonil.sales.stats.dto.StoreCount;
import com.ban.cheonil.sales.stats.dto.StoreHourHeatmap;
import com.ban.cheonil.sales.stats.dto.StoreMenuMix;
import com.ban.cheonil.sales.stats.dto.StoreSales;
import com.ban.cheonil.sales.stats.dto.TrendPoint;
import com.ban.cheonil.store.StoreRepo;
import com.ban.cheonil.store.entity.Store;

import lombok.RequiredArgsConstructor;

/**
 * 통계 서비스 — 주문내역관리 페이지의 통계 탭 4 endpoint 처리.
 *
 * <p>접근 방식: SQL 집계 대신 기간 내 주문 + 결제 + 메뉴 한번에 fetch 후 Java 스트림으로 집계. POS 규모 (월~연 단위에도 ~수만 row) 에선
 * 인메모리 처리가 충분하고 코드도 단순.
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
  private final SettingService settingService;

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

    // hourly — 운영시간 setting 기반 bucket
    OperatingHours hours = settingService.getOperatingHours();
    Map<Integer, Integer> hourMap =
        orders.stream()
            .collect(
                Collectors.groupingBy(
                    o -> o.getOrderAt().getHour(), Collectors.summingInt(Order::getAmount)));
    List<HourBucket> hourlys =
        IntStream.rangeClosed(hours.startHour(), hours.endHour())
            .mapToObj(h -> new HourBucket(h, hourMap.getOrDefault(h, 0)))
            .toList();

    List<StoreSales> storesTop5 = topStores(orders, 5);
    List<PayMethodPart> payParts = aggregatePayParts(orders);
    List<MenuRank> menusTop5 = topMenusByCount(orders, 5);

    return new StatsBasicRes(
        totalSales,
        prevSales,
        totalCount,
        (int) prevCount,
        hourlys,
        storesTop5,
        payParts,
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
   * Menu — 수량 TOP 10 / 판매액 TOP 10 / 카테고리별 / 시간대별 평균 메뉴 개수
   * ========================================================= */

  public StatsMenuRes menu(DateRangeParams params) {
    OffsetDateTime[] cur = dayRange(params.from(), params.to());
    List<Order> orders = orderRepo.findAll(rangeSpec(cur));

    List<MenuRank> menusTop10 = topMenusByCount(orders, 10);
    List<MenuRank> menusTop10ByAmount = topMenusByAmount(orders, 10);
    List<CategoryPart> categoryParts = aggregateCategoryParts(orders);
    StatsHourMenuStack hourlyMenuStack = aggregateHourMenuStack(orders);

    return new StatsMenuRes(menusTop10, menusTop10ByAmount, categoryParts, hourlyMenuStack);
  }

  /* =========================================================
   * Store — 점포별 매출/메뉴 비중/주문 빈도/미수/결제분포
   * ========================================================= */

  public StatsStoreRes store(DateRangeParams params) {
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

    // 점포별 메뉴 mix — 모든 매장 반환, frontend 가 multi-select 로 표시 매장 선택
    List<StoreMenuMix> storeMenuMixes = computeStoreMenuMixes(orders, storeEntities);

    // 시간×매장 heatmap — 모든 매장 9~20 시간대 0 채움
    List<StoreHourHeatmap> storeHourHeatmap = aggregateStoreHourHeatmap(orders, storeEntities);

    return new StatsStoreRes(stores, storeMenuMixes, orderCounts, storeHourHeatmap);
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

  /** menu amount 기준 TOP N — count 도 함께 집계. {@link #topMenusByCount} 와 정렬 기준만 다름. */
  private List<MenuRank> topMenusByAmount(List<Order> orders, int limit) {
    return rankMenus(orders, Comparator.comparingInt((MenuAcc a) -> a.amount).reversed(), limit);
  }

  /** menu count 기준 TOP N. amount 도 함께 집계. */
  private List<MenuRank> topMenusByCount(List<Order> orders, int limit) {
    return rankMenus(orders, Comparator.comparingInt((MenuAcc a) -> a.count).reversed(), limit);
  }

  /** 공통 — 주문들에서 메뉴별 (count, amount) 누적 후 정렬/limit. */
  private List<MenuRank> rankMenus(List<Order> orders, Comparator<MenuAcc> comparator, int limit) {
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
        .sorted(comparator)
        .limit(limit)
        .map(a -> new MenuRank(a.nm, a.count, a.amount))
        .toList();
  }

  /**
   * 시간대별 메뉴 판매 stacked — 시간 × 메뉴 cross-tab.
   *
   * <p>전체 기간 수량 TOP 5 메뉴 추출 + 그 외 "기타" 합산. 각 hour 별로 [TOP1, TOP2, ..., TOP5, 기타] 순서의 cnt 배열을
   * counts 로 노출.
   */
  private StatsHourMenuStack aggregateHourMenuStack(List<Order> orders) {
    if (orders.isEmpty()) return new StatsHourMenuStack(List.of(), List.of());
    List<Long> orderSeqs = orders.stream().map(Order::getSeq).toList();
    List<OrderMenuExtRes> items = orderMenuRepo.findExtsByOrderSeqs(orderSeqs);
    if (items.isEmpty()) return new StatsHourMenuStack(List.of(), List.of());

    // 1. 전체 기간 menu count 누적 → TOP 5 추출
    Map<Short, MenuAcc> totalAcc = new java.util.HashMap<>();
    for (OrderMenuExtRes om : items) {
      MenuAcc acc = totalAcc.computeIfAbsent(om.menuSeq(), k -> new MenuAcc(om.menuNm()));
      acc.count += om.cnt();
    }
    List<Map.Entry<Short, MenuAcc>> sorted =
        totalAcc.entrySet().stream()
            .sorted(
                Comparator.<Map.Entry<Short, MenuAcc>>comparingInt(e -> e.getValue().count)
                    .reversed())
            .toList();
    List<Map.Entry<Short, MenuAcc>> top5 = sorted.stream().limit(5).toList();
    boolean hasEtc = sorted.size() > 5;

    // menus 라벨 순서 (TOP1 → TOP5 → "기타")
    List<String> menuNames = new java.util.ArrayList<>(top5.stream().map(e -> e.getValue().nm).toList());
    if (hasEtc) menuNames.add("기타");
    int slots = menuNames.size();

    // 2. menuSeq → slot 인덱스
    Map<Short, Integer> menuToSlot = new java.util.HashMap<>();
    for (int i = 0; i < top5.size(); i++) menuToSlot.put(top5.get(i).getKey(), i);
    int etcSlot = top5.size();

    // 3. orderSeq → hour
    Map<Long, Integer> hourByOrder =
        orders.stream().collect(Collectors.toMap(Order::getSeq, o -> o.getOrderAt().getHour()));

    // 4. hour → int[slots] 누적
    Map<Integer, int[]> hourCounts = new TreeMap<>();
    for (OrderMenuExtRes om : items) {
      Integer hour = hourByOrder.get(om.orderSeq());
      if (hour == null) continue;
      int[] arr = hourCounts.computeIfAbsent(hour, k -> new int[slots]);
      Integer slot = menuToSlot.get(om.menuSeq());
      int s = slot != null ? slot : (hasEtc ? etcSlot : -1);
      if (s >= 0) arr[s] += om.cnt();
    }

    List<HourMenuStack> hours =
        hourCounts.entrySet().stream()
            .map(
                e ->
                    new HourMenuStack(
                        e.getKey(),
                        java.util.Arrays.stream(e.getValue()).boxed().toList()))
            .toList();
    return new StatsHourMenuStack(menuNames, hours);
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

  /**
   * 시간×매장 heatmap — 각 매장의 9~20 시간대 주문 건수.
   *
   * <p>모든 매장에 대해 동일한 hour bucket 셋 ({@link OperatingHours} 기반) 을 가짐 (해당 시간 미주문이면 0). 모든 매장 반환 →
   * frontend 가 multi-select 로 표시 매장 선택.
   */
  private List<StoreHourHeatmap> aggregateStoreHourHeatmap(
      List<Order> orders, Map<Short, Store> storeEntities) {
    if (orders.isEmpty()) return List.of();

    OperatingHours hours = settingService.getOperatingHours();

    // storeSeq → hour → 주문 건수
    Map<Short, Map<Integer, Long>> grouped =
        orders.stream()
            .collect(
                Collectors.groupingBy(
                    Order::getStoreSeq,
                    Collectors.groupingBy(o -> o.getOrderAt().getHour(), Collectors.counting())));

    return grouped.entrySet().stream()
        .map(
            e -> {
              Map<Integer, Long> hourMap = e.getValue();
              List<StoreHourHeatmap.HourCount> hourly =
                  IntStream.rangeClosed(hours.startHour(), hours.endHour())
                      .mapToObj(
                          h ->
                              new StoreHourHeatmap.HourCount(
                                  h, hourMap.getOrDefault(h, 0L).intValue()))
                      .toList();
              return new StoreHourHeatmap(e.getKey(), storeNm(storeEntities, e.getKey()), hourly);
            })
        .toList();
  }

  /**
   * 모든 매장의 메뉴 mix — 매장당 자체 TOP 5 + 기타. frontend 의 multi-select donut grid 용.
   *
   * <p>기존 {@code computeStoreMenuParts} 와 다른 점: storeSeq 필터 제거 (전체 매장), TOP 4 → TOP 5, storeNm 포함.
   */
  private List<StoreMenuMix> computeStoreMenuMixes(
      List<Order> orders, Map<Short, Store> storeEntities) {
    if (orders.isEmpty()) return List.of();

    Map<Short, List<Order>> byStore =
        orders.stream().collect(Collectors.groupingBy(Order::getStoreSeq));

    List<StoreMenuMix> result = new java.util.ArrayList<>();
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
      List<StoreMenuMix.Item> top5 =
          accMap.values().stream()
              .sorted(Comparator.comparingInt((MenuAcc a) -> a.count).reversed())
              .limit(5)
              .map(a -> new StoreMenuMix.Item(a.nm, a.count, percent(a.count, totalCount)))
              .toList();
      int top5Count = top5.stream().mapToInt(StoreMenuMix.Item::count).sum();
      int etcCount = totalCount - top5Count;
      result.add(new StoreMenuMix(ss, storeNm(storeEntities, ss), top5, etcCount));
    }
    return result;
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
