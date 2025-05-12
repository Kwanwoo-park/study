package spring.study.controller.forbidden;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.entity.member.Member;
import spring.study.service.forbidden.ForbiddenService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/forbidden")
public class ForbiddenViewController {
    private final ForbiddenService forbiddenService;

    @GetMapping("/list")
    public String forbiddenList(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "forbidden/list";
    }
}
