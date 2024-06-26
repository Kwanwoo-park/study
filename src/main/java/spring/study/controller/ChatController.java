package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.ChatMember;
import spring.study.entity.Member;
import spring.study.service.ChatMemberService;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
   private final ChatMessageService chatMessageService;
   private final ChatRoomService chatRoomService;
   private final ChatMemberService chatMemberService;
   private Member member;

   @RequestMapping(value = "/chatList", method = {RequestMethod.GET, RequestMethod.POST})
    public String chatList(Model model, HttpSession session) {
      member = (Member) session.getAttribute("member");
      model.addAttribute("roomList", chatRoomService.findAll());

      return "chat/chatList";
   }

   @PostMapping("/createRoom")
    public String createRoom(Model model, @RequestParam String name) {
       model.addAttribute("room", chatRoomService.createRoom(name));
       model.addAttribute("name", member.getName());
       model.addAttribute("email", member.getEmail());
       model.addAttribute("flag", true);

       return "chat/chatRoom";
   }

   @GetMapping("/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
       boolean flag = true;
       model.addAttribute("room", chatRoomService.findRoom(roomId));
       model.addAttribute("name", member.getName());
       model.addAttribute("email", member.getEmail());
       model.addAttribute("message", chatMessageService.findMessage(roomId));

       for (ChatMember mem : chatMemberService.findMember(roomId)) {
           if (mem.getEmail().equals(member.getEmail())) {
               flag = false;
               break;
           }
       }

       model.addAttribute("flag", flag);

       return "chat/chatRoom";
   }
}
