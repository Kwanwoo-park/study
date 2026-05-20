package spring.study.account.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.service.AccountService;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountViewController {
    private final SessionManager sessionManager;
    private final AccountService accountService;
    private final MemberService memberService;

    @GetMapping
    public String account(
            Model model,
            HttpServletRequest request,
            @RequestParam(value = "tranAccount", required = false) String tranAccount,
            @RequestParam(value = "tranName", required = false) String tranName
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/account";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("tranAccount", tranAccount);
        model.addAttribute("tranName", tranName);

        return "account/account";
    }

    @GetMapping("/transfer")
    public String transfer(
            Model model,
            HttpServletRequest request,
            @RequestParam String email,
            RedirectAttributes redirectAttributes
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/account/transfer";
        }

        Member transferMember = memberService.findMember(email);
        List<AccountResponseDto> transferAccounts = accountService.findByMember(transferMember).stream()
                .map(AccountResponseDto::new)
                .toList();

        if (transferAccounts.size() == 1) {
            redirectAttributes.addAttribute("tranAccount", transferAccounts.get(0).getAccount());
            redirectAttributes.addAttribute("tranName", transferMember.getName());
            return "redirect:/account";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("tranName", transferMember.getName());
        model.addAttribute("transferAccounts", transferAccounts);

        return "account/transfer";
    }
}
