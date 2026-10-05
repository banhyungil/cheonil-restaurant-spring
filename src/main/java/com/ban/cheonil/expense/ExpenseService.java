package com.ban.cheonil.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.common.config.TimeZoneConfig;
import com.ban.cheonil.expense.dto.ExpenseProductReq;
import com.ban.cheonil.expense.dto.ExpenseProductRes;
import com.ban.cheonil.expense.dto.ExpenseRes;
import com.ban.cheonil.expense.dto.ExpenseSaveReq;
import com.ban.cheonil.expense.dto.ExpensesParams;
import com.ban.cheonil.expense.entity.Expense;
import com.ban.cheonil.expense.entity.ExpenseCategory;
import com.ban.cheonil.expense.entity.ExpenseProduct;
import com.ban.cheonil.product.ProductRepo;
import com.ban.cheonil.product.UnitRepo;
import com.ban.cheonil.product.entity.Product;
import com.ban.cheonil.product.entity.Unit;
import com.ban.cheonil.store.StoreRepo;

import lombok.RequiredArgsConstructor;

/**
 * 지출 — 일자 단위로 다룬다 (expense_at 은 KST 자정으로 저장).
 *
 * <p>KST 는 systemDefault 대신 BUSINESS_ZONE 을 직접 쓴다 — JVM 기본 TZ 설정은 main() 에서만 적용된다.
 *
 * <p>같은 일자 + 같은 매장 지출은 하나만 둔다 (uq_expense_store_day). 서버는 자동 병합하지 않고, 프론트가 lookup 으로 기존 지출을
 * 불러와 수정하게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

  /** 지출명 추천 개수. */
  private static final int NAME_SUGGEST_LIMIT = 20;

  private final ExpenseRepo expenseRepo;
  private final ExpenseProductRepo expenseProductRepo;
  private final ExpenseCategoryRepo expenseCategoryRepo;
  private final StoreRepo storeRepo;
  private final ProductRepo productRepo;
  private final UnitRepo unitRepo;

  /** 기간 + 필터 — 최신 일자순. 카테고리는 하위 카테고리 지출까지 포함. */
  public List<ExpenseRes> findAll(ExpensesParams params) {
    Specification<Expense> spec =
        Specification.<Expense>where(
                (r, q, cb) ->
                    cb.and(
                        cb.greaterThanOrEqualTo(r.get("expenseAt"), startOf(params.from())),
                        cb.lessThan(r.get("expenseAt"), startOf(params.to().plusDays(1)))))
            .and(ctgFilter(params.ctgSeq()))
            .and(storeFilter(params.storeSeq()))
            .and(nmFilter(params.q()));
    Sort sort = Sort.by(Sort.Order.desc("expenseAt"), Sort.Order.desc("seq"));
    return toRes(expenseRepo.findAll(spec, sort));
  }

  public ExpenseRes findBySeq(Long seq) {
    return toRes(get(seq));
  }

  /** 같은 일자 + 같은 매장 기존 지출. */
  public Optional<ExpenseRes> lookup(LocalDate date, Short storeSeq) {
    return expenseRepo.findByStoreAndDay(storeSeq, date).map(this::toRes);
  }

  /** 카테고리에서 쓴 지출명 — 최근 사용순. */
  public List<String> recentNames(Integer ctgSeq) {
    return expenseRepo.findRecentNames(ctgSeq, NAME_SUGGEST_LIMIT);
  }

  @Transactional
  public ExpenseRes create(ExpenseSaveReq req) {
    Expense e = new Expense();
    apply(e, req);
    OffsetDateTime now = OffsetDateTime.now();
    e.setRegAt(now);
    e.setModAt(now);
    expenseRepo.save(e);
    replaceProducts(e.getSeq(), req.products());
    return toRes(e);
  }

  /** 전체 교체 (PUT). */
  @Transactional
  public ExpenseRes update(Long seq, ExpenseSaveReq req) {
    Expense e = get(seq);
    apply(e, req);
    e.setModAt(OffsetDateTime.now());
    expenseRepo.flush();
    replaceProducts(seq, req.products());
    return toRes(get(seq));
  }

  /** 지출 삭제 — 품목도 함께 삭제. */
  @Transactional
  public void remove(Long seq) {
    Expense e = get(seq);
    expenseProductRepo.deleteByExpsSeq(seq);
    expenseRepo.delete(e);
  }

  /* ==================== helpers ==================== */

  private Expense get(Long seq) {
    return expenseRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("expense " + seq + " not found"));
  }

  private void apply(Expense e, ExpenseSaveReq req) {
    if (!expenseCategoryRepo.existsById(req.ctgSeq())) {
      throw new EntityNotFoundException("expense category " + req.ctgSeq() + " not found");
    }
    if (req.storeSeq() != null) {
      if (!storeRepo.existsById(req.storeSeq())) {
        throw new EntityNotFoundException("store " + req.storeSeq() + " not found");
      }
      // 같은 일자 + 같은 매장 지출이 이미 있으면 차단 — DB 유니크 인덱스보다 먼저 알기 쉬운 메시지로
      expenseRepo
          .findByStoreAndDay(req.storeSeq(), req.expenseDt())
          .filter(other -> !other.getSeq().equals(e.getSeq()))
          .ifPresent(
              other -> {
                throw new IllegalStateException("같은 날 같은 매장의 지출이 이미 있습니다. 기존 지출을 수정해주세요.");
              });
    }
    e.setCtgSeq(req.ctgSeq());
    e.setStoreSeq(req.storeSeq());
    e.setNm(req.nm().trim());
    e.setAmount(req.amount());
    e.setExpenseAt(startOf(req.expenseDt()));
    e.setCmt(req.cmt());
  }

  /**
   * 품목 전체 교체 — 벌크 삭제 후 삽입.
   *
   * <p>단위수량 미사용 단위의 제품이면 unitCnt 를 비운다. 같은 (제품, 규격) 이 두 줄이면 차단 (프론트가 합쳐서 보낸다).
   */
  private void replaceProducts(Long expsSeq, List<ExpenseProductReq> reqs) {
    expenseProductRepo.deleteByExpsSeq(expsSeq);
    if (reqs == null || reqs.isEmpty()) return;

    Map<Integer, Product> products =
        productRepo.findAllById(reqs.stream().map(ExpenseProductReq::prdSeq).toList()).stream()
            .collect(Collectors.toMap(Product::getSeq, p -> p));
    Map<Short, Unit> units =
        unitRepo.findAllById(products.values().stream().map(Product::getUnitSeq).toList()).stream()
            .collect(Collectors.toMap(Unit::getSeq, u -> u));

    Set<String> keys = new HashSet<>();
    List<ExpenseProduct> lines =
        reqs.stream()
            .map(
                r -> {
                  Product prd = products.get(r.prdSeq());
                  if (prd == null) {
                    throw new EntityNotFoundException("product " + r.prdSeq() + " not found");
                  }
                  BigDecimal unitCnt =
                      units.get(prd.getUnitSeq()).getIsUnitCnt() && r.unitCnt() != null
                          ? r.unitCnt().setScale(2, RoundingMode.UNNECESSARY)
                          : null;
                  if (!keys.add(r.prdSeq() + ":" + unitCnt)) {
                    throw new IllegalArgumentException("같은 제품·규격이 구입목록에 두 번 들어있습니다.");
                  }
                  ExpenseProduct line = new ExpenseProduct();
                  line.setExpsSeq(expsSeq);
                  line.setPrdSeq(r.prdSeq());
                  line.setCnt(r.cnt().shortValue());
                  line.setPrice(r.price());
                  line.setUnitCnt(unitCnt);
                  line.setCmt(r.cmt());
                  return line;
                })
            .toList();
    expenseProductRepo.saveAll(lines);
  }

  private ExpenseRes toRes(Expense e) {
    return toRes(List.of(e)).getFirst();
  }

  /** 품목은 지출 목록 전체를 한 번의 IN 쿼리로 묶어 가져온다. */
  private List<ExpenseRes> toRes(List<Expense> expenses) {
    if (expenses.isEmpty()) return List.of();
    Map<Long, List<ExpenseProductRes>> linesByExps =
        expenseProductRepo
            .findByExpsSeqInOrderBySeq(expenses.stream().map(Expense::getSeq).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    ExpenseProduct::getExpsSeq,
                    Collectors.mapping(ExpenseProductRes::from, Collectors.toList())));
    return expenses.stream()
        .map(e -> ExpenseRes.from(e, linesByExps.getOrDefault(e.getSeq(), List.of())))
        .toList();
  }

  private static OffsetDateTime startOf(LocalDate date) {
    return date.atStartOfDay(TimeZoneConfig.BUSINESS_ZONE).toOffsetDateTime();
  }

  /** 카테고리 + 하위 카테고리. 카테고리는 행 수가 적어 전체를 읽어 path 로 거른다. */
  private Specification<Expense> ctgFilter(Integer ctgSeq) {
    if (ctgSeq == null) return (r, q, cb) -> cb.conjunction();
    List<ExpenseCategory> all = expenseCategoryRepo.findAllOrderByPath();
    String path =
        all.stream()
            .filter(c -> c.getSeq().equals(ctgSeq))
            .map(ExpenseCategory::getPath)
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("expense category " + ctgSeq + " not found"));
    Set<Integer> seqs =
        all.stream()
            .filter(c -> c.getPath().equals(path) || c.getPath().startsWith(path + "."))
            .map(ExpenseCategory::getSeq)
            .collect(Collectors.toSet());
    return (r, q, cb) -> r.get("ctgSeq").in(seqs);
  }

  private Specification<Expense> storeFilter(Short storeSeq) {
    if (storeSeq == null) return (r, q, cb) -> cb.conjunction();
    return (r, q, cb) -> cb.equal(r.get("storeSeq"), storeSeq);
  }

  private Specification<Expense> nmFilter(String keyword) {
    if (keyword == null || keyword.isBlank()) return (r, q, cb) -> cb.conjunction();
    return (r, q, cb) -> cb.like(r.get("nm"), "%" + keyword.trim() + "%");
  }
}
