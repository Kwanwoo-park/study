package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.board.service.BoardService;
import spring.study.follow.service.FollowService;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
@Slf4j
public class MemberViewController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final FollowService followService;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpServletRequest request) {

        HttpSession session = request.getSession();

        if (session.getAttribute("member") != null) {
            Member member = (Member) request.getSession().getAttribute("member");

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

        Member member;

        if (session != null && request.isRequestedSessionIdValid() && session.getAttribute("member") != null) {
            if (email.equals(((Member) session.getAttribute("member")).getEmail())) {
                member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());
                session.setAttribute("member", member);
            }
            else
                return "redirect:/member/search/detail?email=" + email;

            if (member == null) {
                session.invalidate();
                return "redirect:/member/login?error=true&exception=Not Found account";
            }

            if (!memberService.validateSession(request)) {
                session.invalidate();
                return "redirect:/member/login?error=true&exception=Session Invalid";
            }

            model.addAttribute("member", member);
            model.addAttribute("resultMap", boardService.findByMember(member));
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

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        model.addAttribute("email", member.getEmail());

        return "member/updatePassword";
    }

    @GetMapping("/updatePhone")
    public String updatePhone(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

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

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        model.addAttribute("name", member.getName());

        return "member/withdrawal";
    }

    @GetMapping("/search")
    public String memberFind(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_find";
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        if (member.getEmail().equals(search_member.getEmail()))
            return "redirect:/member/detail?email="+search_member.getEmail();
        else
            session.setAttribute("member", member);

        member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

        model.addAttribute("member", search_member);
        model.addAttribute("resultMap", boardService.findByMember(search_member));
        model.addAttribute("status", followService.existFollow(member, search_member));
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_detail";
    }
}
