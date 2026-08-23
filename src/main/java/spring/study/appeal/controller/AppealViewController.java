package spring.study.appeal.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@Controller
@RequiredArgsConstructor
public class AppealViewController {
    private final JwtManager jwtManager;

    @GetMapping("/appeal")
    public String appeal(Model model,
                         @RequestParam(required = false) String email,
                         HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        model.addAttribute("authenticated", member != null);
        model.addAttribute("email", member == null ? (email == null ? "" : email.trim()) : member.getEmail());
        model.addAttribute("name", member == null ? "" : member.getName());
        return "appeal/form";
    }
}
