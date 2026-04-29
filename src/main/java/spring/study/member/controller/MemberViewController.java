package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.SessionManager;
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
    private final SessionManager sessionManager;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);

        if (member != null) {
            if (member.getRole() == Role.USER) return "redirect:/board/main";
            else if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
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
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        String memberEmail = member.getEmail();

        if (!email.equals(memberEmail))
            return "redirect:/member/search/detail?email=" + email;

        model.addAttribute("member", member);
        model.addAttribute("board_count", boardService.countByMember(member));
        model.addAttribute("follower_count", followService.countFollowers(member));
        model.addAttribute("following_count", followService.countFollowing(member));

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
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("email", member.getEmail());

        return "member/updatePassword";
    }

    @GetMapping("/updatePhone")
    public String updatePhone(Model model, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

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
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("name", member.getName());

        return "member/withdrawal";
    }

    @GetMapping("/search")
    public String memberFind(Model model, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_find";
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        String memberEmail = member.getEmail();

        if (memberEmail.equals(memberRequestDto.getEmail()))
            return "redirect:/member/detail?email=" + memberEmail;

        Member search_member = memberService.findMember(memberRequestDto.getEmail());

        model.addAttribute("member", search_member);
        model.addAttribute("status", followService.existFollow(member, search_member));
        model.addAttribute("board_count", boardService.countByMember(search_member));
        model.addAttribute("follower_count", followService.countFollowers(search_member));
        model.addAttribute("following_count", followService.countFollowing(search_member));
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_detail";
    }
}
