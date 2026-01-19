package spring.study.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.NotificationService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/notification")
public class NotificationViewController {
    private final MemberService memberService;
    private final NotificationService notificationService;

    @GetMapping("/list")
    public String getNotification(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        model.addAttribute("member", member);

        return "notification/list";
    }
}
