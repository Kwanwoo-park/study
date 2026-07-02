package spring.study.report.controller;

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
@RequestMapping("/report")
public class ReportViewController {
    private final SessionManager sessionManager;

    @GetMapping
    public String getReportPage(Model model,
                                HttpServletRequest request,
                                @RequestParam(required = false) String targetType,
                                @RequestParam(required = false) String targetId) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/report";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("targetType", targetType);
        model.addAttribute("targetId", targetId);

        return "report/form";
    }

    @GetMapping("/my")
    public String getMyReportPage(Model model, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/report/my";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "report/my";
    }
}
