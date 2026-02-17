package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.chat.entity.ChatRoom;
import spring.study.common.service.SessionService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {
    private final SessionService sessionService;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;

    @GetMapping("/chatList")
    public String chatList(Model model,
                           HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

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
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        ChatRoom room = roomService.find(roomId);

        if (room == null)
            return "redirect:/chat/chatList";

        model.addAttribute("room", room.getRoomId());
        model.addAttribute("member", member.getEmail());
        model.addAttribute("flag", !roomMemberService.exist(member, room));

        return "chat/chatRoom";
    }
}
