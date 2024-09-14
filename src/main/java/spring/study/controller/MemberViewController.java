package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.alert.AlertMessage;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.BoardService;
import spring.study.service.FollowService;
import spring.study.service.MemberService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
@Slf4j
public class MemberViewController {
    private final MemberService memberService;
    private final FollowService followService;
    private final BoardService boardService;
    private Member member;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpSession session) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        if (session != null) {
            member = (Member) session.getAttribute("member");

            if (member == null)
                session.invalidate();
            else {
                if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
                return "redirect:/board/list";
            }
        }

        return "/member/login";
    }

    @GetMapping("/login/action")
    public String loginAction(MemberRequestDto dto, HttpSession session, Model model) {
        try {
            member = (Member) memberService.loadUserByUsername(dto.getEmail());

            if (new BCryptPasswordEncoder().matches(dto.getPassword(), member.getPassword())){
                if (member.getRole() != Role.DENIED) {
                    memberService.updateLastLoginTime(member.getId());
                    session.setAttribute("member", member);
                }
            }
            else {
                return "redirect:/member/login?error=true&exception=Invalid Email or Password";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/member/login?error=true&exception=Invalid Email or Password";
        }

        AlertMessage message;

        if (member.getRole() == Role.ADMIN) {
            message = new AlertMessage(member.getName() + " 관리자님 환영합니다.", "/admin/administrator", RequestMethod.GET, null);
        }
        else if (member.getRole() == Role.USER){
            message = new AlertMessage(member.getName() + "님 환영합니다.", "/board/list", RequestMethod.GET, null);
        }
        else {
            return "redirect:/member/login?error=true&exception=Accept Denied";
        }


        return message.showMessageAndRedirect(model);
    }

    @GetMapping("/register")
    public String register() {
        return "/member/register";
    }

    @GetMapping("/detail")
    public String detail(Model model, HttpSession session){
        if (session != null) {
            member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

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
    public String updatePassword(Model model, HttpSession session) throws Exception {
        member = (Member) session.getAttribute("member");

        session.invalidate();

        model.addAttribute("email", member.getEmail());

        return "/member/updatePassword";
    }

    @GetMapping("/withdrawal")
    public String withdrawal(Model model, HttpSession session) {
        if (session == null || member == null)
            return "redirect:/login?error=true&exception=Not Found account";

        model.addAttribute("name", member.getName());

        return "/member/withdrawal";
    }

    @GetMapping("/search")
    public String memberFind(Model model, HttpSession session) {
        HashMap<String, Object> member_search = (HashMap<String, Object>) session.getAttribute("member_search");

        model.addAttribute("member", member_search);

        return "/member/member_find";
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto, HttpSession session) {
        boolean status = false;
        Member search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        if (session == null)
            return "redirect:/member/login?error=true&exception=Not Found account";

        member = memberService.findMember(((Member) session.getAttribute("member")).getEmail());

        if (member.getEmail().equals(search_member.getEmail()))
            return "redirect:/member/detail";

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

        return "/member/member_detail";
    }
}
