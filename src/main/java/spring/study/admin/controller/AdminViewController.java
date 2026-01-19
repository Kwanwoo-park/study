package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminViewController {
    private final MemberService memberService;
    private final ForbiddenService forbiddenService;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;

    @GetMapping("/administrator")
    public String admin(HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "admin/administrator";
    }

    @GetMapping("/memberCheck")
    public String member_check(Model model, HttpServletRequest request,
                               @RequestParam(required = false, defaultValue = "0") Integer page,
                               @RequestParam(required = false, defaultValue = "5") Integer size){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findAll(page, size));

        return "admin/member_check";
    }

    @GetMapping("/member/detail")
    public String memberDetail(Model model, MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (session.getAttribute("member") == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findMember(requestDto.getEmail()));

        return "admin/update_member";
    }

    @GetMapping("/forbidden/word/list")
    public String forbiddenWordList(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (session.getAttribute("member") == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("list", forbiddenService.findAll());

        return "admin/forbidden_word_list";
    }

    @GetMapping("/forbidden/word/apply")
    public String forbiddenWordAppplyList(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return "redirect:/member/login?error=true&exception=Not Found account";

        if (session.getAttribute("member") == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("list", forbiddenService.findByStatusNot(Status.APPROVAL));

        return "admin/forbidden_word_apply";
    }

    @GetMapping("/chatTest")
    public String chatList(Model model,
                           @RequestParam(required = false, defaultValue = "0") Integer page,
                           @RequestParam(required = false, defaultValue = "5") Integer size,
                           HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        List<ChatRoom> list = roomMemberService.findRoom(member);

        model.addAttribute("roomList", list);
        model.addAttribute("member", roomMemberService.findMember(list, member));

        return "admin/chatTest";
    }
}
