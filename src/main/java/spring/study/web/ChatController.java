package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.chat.ChatMessageRequestDto;
import spring.study.entity.chat.ChatMember;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.member.Member;
import spring.study.service.ChatService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {
   private final ChatService chatService;
   private Member member;

   @RequestMapping("/chat/chatList")
    public String chatList(Model model, HttpServletRequest request) {
       List<ChatRoom> roomList = chatService.findAll();

       HttpSession session = request.getSession();
       member = (Member) session.getAttribute("member");

       model.addAttribute("roomList", roomList);

       return "chat/chatList";
   }

   @PostMapping("/chat/createRoom")
    public String createRoom(Model model, @RequestParam String name) {
       ChatRoom room = chatService.createRoom(name);

       model.addAttribute("room", room);
       model.addAttribute("member", member);

       return "chat/chatRoom";
   }

   @GetMapping("/chat/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
       ChatRoom room = chatService.findRoom(roomId);

       model.addAttribute("room", room);
       model.addAttribute("member", member);

       return "chat/chatRoom";
   }
}
