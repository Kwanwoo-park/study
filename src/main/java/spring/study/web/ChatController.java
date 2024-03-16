package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.chat.ChatMember;
import spring.study.entity.member.Member;
import spring.study.service.ChatService;


@Controller
@RequiredArgsConstructor
public class ChatController {
   private final ChatService chatService;
   private Member member;

   @RequestMapping(value = "/chat/chatList", method = {RequestMethod.GET, RequestMethod.POST})
    public String chatList(Model model, HttpServletRequest request) {
      HttpSession session = request.getSession();
      member = (Member) session.getAttribute("member");
      model.addAttribute("roomList", chatService.findAll());

      return "chat/chatList";
   }

   @PostMapping("/chat/createRoom")
    public String createRoom(Model model, @RequestParam String name) {
      model.addAttribute("room", chatService.createRoom(name));
      model.addAttribute("member", member);
      model.addAttribute("flag", true);

      return "chat/chatRoom";
   }

   @GetMapping("/chat/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
       boolean flag = true;
      model.addAttribute("room", chatService.findRoom(roomId));
      model.addAttribute("member", member);
      model.addAttribute("message", chatService.findMessage(roomId));

      for (ChatMember mem : chatService.findMember(roomId)) {
          if (mem.getEmail().equals(member.getEmail())) {
              flag = false;
              break;
          }
      }

      model.addAttribute("flag", flag);

      return "chat/chatRoom";
   }
}
