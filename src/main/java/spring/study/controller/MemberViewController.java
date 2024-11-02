package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.MemberService;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
@Slf4j
public class MemberViewController {
    private final MemberService memberService;
    private Member member;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpServletRequest request) {

        if (request.getSession().getAttribute("member") != null) {
            member = (Member) request.getSession().getAttribute("member");

            if (member.getRole() == Role.USER) return "redirect:/board/list";
            else if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
            else request.getSession().invalidate();
        }

        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        return "/member/login";
    }

    @GetMapping("/register")
    public String register() {
        return "/member/register";
    }

    @GetMapping("/detail")
    public String detail(Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session != null && request.isRequestedSessionIdValid()) {
            member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

            if (member == null) {
                session.invalidate();
                return "redirect:/member/login?error=true&exception=Not Found account";
            }

            model.addAttribute("member", member);
            model.addAttribute("board", member.getBoard().size());
            model.addAttribute("follower", member.getFollowing().size());
            model.addAttribute("following", member.getFollower().size());
            model.addAttribute("list", member.getBoard());

            session.setAttribute("member", member);
        }
        else {
            return "redirect:/member/login?error=true&exception=Not Found account";
        }

        return "/member/detail";
    }

    @GetMapping("/find")
    public String find(Model model) {
        return "/member/find";
    }

    @GetMapping("/findByEmail")
    public String findByEmail(Model model) {
        return "/member/email_find";
    }

    @GetMapping("/findByInfo")
    public String findByInfo(Model model) {
        return "/member/info_find";
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

        return "/member/updatePassword";
    }

    @GetMapping("/updatePassword/{email}")
    public String updatePassword(@PathVariable String email, Model model) throws Exception {
        model.addAttribute("email", email);

        return "/member/updatePassword";
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

        return "/member/withdrawal";
    }

    @GetMapping("/search")
    public String memberFind(Model model) {
        return "/member/member_find";
    }

    @GetMapping("/search/{name}")
    public String memberFind(@PathVariable String name, Model model) {
        HashMap<String, Object> resultMap = memberService.findName(name);

        model.addAttribute("member", resultMap);

        return "/member/member_find";
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        boolean status = false;
        Member search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (session.getAttribute("member") == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (((Member) session.getAttribute("member")).getEmail().equals(search_member.getEmail()))
            return "redirect:/member/detail";

        member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

        List<Follow> follower = member.getFollower();

        model.addAttribute("member", search_member);
        model.addAttribute("board", search_member.getBoard().size());
        model.addAttribute("follower", search_member.getFollowing().size());
        model.addAttribute("following", search_member.getFollower().size());
        model.addAttribute("list", search_member.getBoard());

        for (Follow f : follower) {
            if (f.getFollowing().getEmail().equals(search_member.getEmail())) {
                status = true;
                break;
            }
        }

        model.addAttribute("status", status);

        session.setAttribute("member", member);

        return "/member/member_detail";
    }
}
