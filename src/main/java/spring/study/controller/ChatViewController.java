package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.ChatRoom;
import spring.study.entity.Member;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomMemberService;
import spring.study.service.ChatRoomService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {
    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final ChatRoomMemberService roomMemberService;
    private Member member;

    @RequestMapping(value = "/chatList", method = {RequestMethod.GET, RequestMethod.POST})
    public String chatList(Model model,
                           @RequestParam(required = false, defaultValue = "0") Integer page,
                           @RequestParam(required = false, defaultValue = "5") Integer size,
                           HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("roomList", roomService.findAll(page, size));

        return "chat/chatList";
    }

    @GetMapping("/chatRoom")
    public String chatRoom(@RequestParam String roomId, Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        ChatRoom room = roomService.find(roomId);

        model.addAttribute("room", room);
        model.addAttribute("member", member);
        model.addAttribute("message", messageService.find(room));
        model.addAttribute("flag", roomMemberService.find(member, room) == null);

        return "chat/chatRoom";
    }
}
