package spring.study.account.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.service.AccountService;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountViewFacade {
    private final JwtManager jwtManager;
    private final AccountService accountService;
    private final MemberService memberService;

    public String account(Model model, HttpServletRequest request, String tranAccount, String tranName) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/account";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("tranAccount", tranAccount);
        model.addAttribute("tranName", tranName);

        return "account/account";
    }

    public String transfer(Model model, HttpServletRequest request, String email, RedirectAttributes redirectAttributes) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/account/transfer";
        }

        Member transferMember = memberService.findMember(email);
        List<AccountResponseDto> transferAccounts = accountService.findActiveByMember(transferMember);

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

    public String transactions(Model model, HttpServletRequest request, String account) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/account/transactions";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("account", account);

        return "account/transactions";
    }
}
