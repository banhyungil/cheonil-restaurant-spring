package com.ban.cheonil.expense;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.common.config.TimeZoneConfig;
import com.ban.cheonil.expense.dto.ExpenseRes;
import com.ban.cheonil.expense.dto.ExpenseSaveReq;
import com.ban.cheonil.expense.dto.ExpensesParams;
import com.ban.cheonil.expense.entity.Expense;
import com.ban.cheonil.expense.entity.ExpenseCategory;
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
    return expenseRepo.findAll(spec, sort).stream().map(ExpenseRes::from).toList();
  }

  public ExpenseRes findBySeq(Long seq) {
    return ExpenseRes.from(get(seq));
  }

  /** 같은 일자 + 같은 매장 기존 지출. */
  public Optional<ExpenseRes> lookup(LocalDate date, Short storeSeq) {
    return expenseRepo.findByStoreAndDay(storeSeq, date).map(ExpenseRes::from);
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
    return ExpenseRes.from(expenseRepo.save(e));
  }

  /** 전체 교체 (PUT). */
  @Transactional
  public ExpenseRes update(Long seq, ExpenseSaveReq req) {
    Expense e = get(seq);
    apply(e, req);
    e.setModAt(OffsetDateTime.now());
    return ExpenseRes.from(e);
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
