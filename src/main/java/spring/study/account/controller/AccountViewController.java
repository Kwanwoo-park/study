package spring.study.account.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountViewController {
    private final SessionManager sessionManager;

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
}
