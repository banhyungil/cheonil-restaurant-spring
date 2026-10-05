package com.ban.cheonil.expense;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.expense.dto.ExpenseCategoryRes;
import com.ban.cheonil.expense.dto.ExpenseCategorySaveReq;
import com.ban.cheonil.expense.entity.ExpenseCategory;

import lombok.RequiredArgsConstructor;

/**
 * 지출 카테고리 (ltree 계층). path 라벨은 seq — 예: 식자재(1) > 채소(5) = "1.5".
 *
 * <p>카테고리는 행 수가 적어 검증(형제 이름 중복, 하위 존재 등)은 전체를 읽어 Java 에서 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCategoryService {

  private final ExpenseCategoryRepo expenseCategoryRepo;
  private final ExpenseRepo expenseRepo;

  public List<ExpenseCategoryRes> findAll() {
    return expenseCategoryRepo.findAllOrderByPath().stream()
        .map(ExpenseCategoryRes::from)
        .toList();
  }

  @Transactional
  public ExpenseCategoryRes create(ExpenseCategorySaveReq req) {
    List<ExpenseCategoryRes> all = findAll();
    String parentPath = parentPath(all, req.parentSeq());
    validateSiblingNm(all, req.parentSeq(), req.nm(), null);

    int seq = (int) expenseCategoryRepo.nextSeq();
    String path = parentPath == null ? String.valueOf(seq) : parentPath + "." + seq;
    expenseCategoryRepo.insert(seq, path, req.nm());
    return ExpenseCategoryRes.from(get(seq));
  }

  /** 전체 교체 (PUT) — 이름 변경 + parentSeq 가 바뀌면 하위 트리째 이동. */
  @Transactional
  public ExpenseCategoryRes update(Integer seq, ExpenseCategorySaveReq req) {
    List<ExpenseCategoryRes> all = findAll();
    ExpenseCategoryRes cur = find(all, seq);
    validateSiblingNm(all, req.parentSeq(), req.nm(), seq);

    if (!Objects.equals(cur.parentSeq(), req.parentSeq())) {
      String newParentPath = parentPath(all, req.parentSeq());
      if (newParentPath != null
          && (newParentPath.equals(cur.path()) || newParentPath.startsWith(cur.path() + "."))) {
        throw new IllegalArgumentException("자기 자신이나 하위 카테고리로 이동할 수 없습니다.");
      }
      if (newParentPath == null) {
        expenseCategoryRepo.moveToRoot(cur.path());
      } else {
        expenseCategoryRepo.moveUnder(cur.path(), newParentPath);
      }
    }

    ExpenseCategory c = get(seq);
    c.setNm(req.nm());
    return ExpenseCategoryRes.from(c);
  }

  @Transactional
  public void remove(Integer seq) {
    List<ExpenseCategoryRes> all = findAll();
    find(all, seq);
    if (all.stream().anyMatch(c -> seq.equals(c.parentSeq()))) {
      throw new IllegalStateException("하위 카테고리가 있어 삭제할 수 없습니다.");
    }
    long cnt = expenseRepo.countByCtgSeq(seq);
    if (cnt > 0) {
      throw new IllegalStateException("지출 " + cnt + "건이 연결된 카테고리라 삭제할 수 없습니다.");
    }
    expenseCategoryRepo.deleteById(seq);
  }

  /* ==================== helpers ==================== */

  private ExpenseCategory get(Integer seq) {
    return expenseCategoryRepo
        .findById(seq)
        .orElseThrow(() -> new EntityNotFoundException("expense category " + seq + " not found"));
  }

  private ExpenseCategoryRes find(List<ExpenseCategoryRes> all, Integer seq) {
    return all.stream()
        .filter(c -> c.seq().equals(seq))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("expense category " + seq + " not found"));
  }

  /** 상위 카테고리 path. parentSeq null 이면 최상위라 null. */
  private String parentPath(List<ExpenseCategoryRes> all, Integer parentSeq) {
    return parentSeq == null ? null : find(all, parentSeq).path();
  }

  /** 같은 상위 아래 이름 중복 금지. excludeSeq 는 수정 시 자기 자신. */
  private void validateSiblingNm(
      List<ExpenseCategoryRes> all, Integer parentSeq, String nm, Integer excludeSeq) {
    boolean dup =
        all.stream()
            .anyMatch(
                c ->
                    Objects.equals(c.parentSeq(), parentSeq)
                        && c.nm().equals(nm)
                        && !c.seq().equals(excludeSeq));
    if (dup) {
      throw new IllegalStateException("같은 상위 카테고리에 이미 존재하는 이름입니다: " + nm);
    }
  }
}
