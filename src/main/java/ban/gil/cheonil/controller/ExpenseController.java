package ban.gil.cheonil.controller;

import ban.gil.cheonil.service.ExpenseService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;


}
