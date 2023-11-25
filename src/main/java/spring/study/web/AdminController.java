package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import spring.study.entity.member.Member;
import spring.study.service.MemberService;

@RequiredArgsConstructor
@Controller
public class AdminController {
    private final MemberService memberService;
    private Member member;

    @GetMapping("/admin/administrator")
    public String admin(HttpServletRequest request){
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(60);
        member = (Member) session.getAttribute("member");


        return "/admin/administrator";
    }

    @GetMapping("/admin/member_check")
    public String member_check(Model model) throws Exception {
        try {
            model.addAttribute("member", memberService.findAll());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/admin/member_check";
    }
}
