package spring.study.controller.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.service.chat.ChatMessageService;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.chat.ChatRoomService;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {
    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final ChatRoomMemberService roomMemberService;
    private Member member;

    @GetMapping("/chatList")
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

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        if (member.getRole().equals(Role.ADMIN)) {
            model.addAttribute("roomList", roomService.findAll());
            return "chat/adminChatList";
        }
        else {
            List<ChatRoom> list = roomMemberService.findRoom(member);

            model.addAttribute("roomList", list);
            model.addAttribute("member", roomMemberService.findMember(list, member));

            return "chat/chatList";
        }
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
        model.addAttribute("flag", !roomMemberService.exist(member, room));

        return "chat/chatRoom";
    }
}
