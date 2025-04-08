package spring.study.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.service.board.BoardService;
import spring.study.service.follow.FollowService;
import spring.study.service.member.MemberService;

import java.util.HashMap;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
@Slf4j
public class MemberViewController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final FollowService followService;
    private Member member;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpServletRequest request) {

        HttpSession session = request.getSession();

        if (session.getAttribute("member") != null) {
            member = (Member) request.getSession().getAttribute("member");

            if (member.getRole() == Role.USER) return "redirect:/board/main";
            else if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
            else request.getSession().invalidate();
        }

        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        return "member/login";
    }

    @GetMapping("/register")
    public String register() {
        return "member/register";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam String email, Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session != null && request.isRequestedSessionIdValid() && session.getAttribute("member") != null) {
            if (email.equals(((Member) session.getAttribute("member")).getEmail()))
                member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());
            else {
                return "redirect:/member/search/detail?email=" + email;
            }

            if (member == null) {
                session.invalidate();
                return "redirect:/member/login?error=true&exception=Not Found account";
            }

            model.addAttribute("member", member);
            model.addAttribute("resultMap", boardService.findByMember(member));

            session.setAttribute("member", member);
        }
        else {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        return "member/detail";
    }

    @GetMapping("/find")
    public String find() {
        return "member/find";
    }

    @GetMapping("/findByEmail")
    public String findByEmail() {
        return "member/email_find";
    }

    @GetMapping("/findByInfo")
    public String findByInfo() {
        return "member/info_find";
    }

    @GetMapping("/updatePassword")
    public String updatePassword(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        if (member == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        model.addAttribute("email", member.getEmail());

        return "member/updatePassword";
    }

    @GetMapping("/updatePhone")
    public String updatePhone(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        if (member == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("phone", member.getPhone());
        model.addAttribute("birth", member.getBirth());

        return "member/updatePhone";
    }

    @GetMapping("/updatePassword/{email}")
    public String updatePassword(@PathVariable String email, Model model) throws Exception {
        model.addAttribute("email", email);

        return "member/updatePassword";
    }

    @GetMapping("/withdrawal")
    public String withdrawal(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        if (member == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        model.addAttribute("name", member.getName());

        return "member/withdrawal";
    }

    @GetMapping("/search")
    public String memberFind(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_find";
    }

    @GetMapping("/search/{name}")
    public String memberFind(@PathVariable String name,
                             Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        HashMap<String, Object> resultMap = memberService.findName(name);

        model.addAttribute("member", resultMap);
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_find";
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        Member search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (session.getAttribute("member") == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (((Member) session.getAttribute("member")).getEmail().equals(search_member.getEmail()))
            return "redirect:/member/detail?email="+search_member.getEmail();

        member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

        model.addAttribute("member", search_member);
        model.addAttribute("resultMap", boardService.findByMember(search_member));
        model.addAttribute("status", followService.existFollow(member, search_member));
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        session.setAttribute("member", member);

        return "member/member_detail";
    }
}
