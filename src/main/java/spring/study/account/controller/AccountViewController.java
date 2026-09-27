package spring.study.account.controller;

import spring.study.account.facade.AccountViewFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountViewController {
    private final AccountViewFacade facade;

    @GetMapping
    public String account(Model model, HttpServletRequest request, @RequestParam(value = "tranAccount", required = false) String tranAccount, @RequestParam(value = "tranName", required = false) String tranName) {
        return facade.account(model, request, tranAccount, tranName);
    }

    @GetMapping("/transfer")
    public String transfer(Model model, HttpServletRequest request, @RequestParam String email, RedirectAttributes redirectAttributes) {
        return facade.transfer(model, request, email, redirectAttributes);
    }

    @GetMapping("/transactions")
    public String transactions(Model model, HttpServletRequest request, @RequestParam String account) {
        return facade.transactions(model, request, account);
    }
}
