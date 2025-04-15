package spring.study.controller.notification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.notification.NotificationService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/notification")
public class NotificationViewController {
    private final NotificationService notificationService;

    @GetMapping("/list")
    public String getNotification(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        Member member = (Member) session.getAttribute("member");

        if (member == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        List<Notification> notifications = notificationService.findByMember(member);

        model.addAttribute("notification", notifications);
        model.addAttribute("member", member);

        return "notification/list";
    }
}
