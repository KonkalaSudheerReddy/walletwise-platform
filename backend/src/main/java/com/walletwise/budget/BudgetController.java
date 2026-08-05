package com.walletwise.budget;

import com.walletwise.budget.BudgetDtos.BudgetResponse;
import com.walletwise.budget.BudgetDtos.CreateBudgetRequest;
import com.walletwise.budget.BudgetDtos.UpdateBudgetRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
  private final BudgetService budgets;

  public BudgetController(BudgetService budgets) {
    this.budgets = budgets;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  BudgetResponse create(@Valid @RequestBody CreateBudgetRequest request) {
    return budgets.create(request);
  }

  @GetMapping
  List<BudgetResponse> list(@RequestParam(required = false) String month) {
    return budgets.list(month);
  }

  @GetMapping("/{id}")
  BudgetResponse get(@PathVariable UUID id) {
    return budgets.get(id);
  }

  @PatchMapping("/{id}")
  BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest request) {
    return budgets.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    budgets.delete(id);
  }
}
