package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.service.ChatRoomMemberService;
import spring.study.service.ChatRoomService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {
    private final ChatRoomService roomService;
    private Member member;

    @RequestMapping(value = "/chatList", method = {RequestMethod.GET, RequestMethod.POST})
    public String chatList(Model model,
                           @RequestParam(required = false, defaultValue = "0") Integer page,
                           @RequestParam(required = false, defaultValue = "5") Integer size,
                           HttpSession session) {
        if (session == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("roomList", roomService.findAll(page, size));

        return "chat/chatList";
    }

    @GetMapping("/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
        return "chat/chatRoom";
    }
}
