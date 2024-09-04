package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.Member;


@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

   private Member member;

   @RequestMapping(value = "/chatList", method = {RequestMethod.GET, RequestMethod.POST})
    public String chatList(Model model, HttpSession session) {
      member = (Member) session.getAttribute("member");

      return "chat/chatList";
   }

   @PostMapping("/createRoom")
    public String createRoom(Model model, @RequestParam String name) {
       return "chat/chatRoom";
   }

   @GetMapping("/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
       return "chat/chatRoom";
   }
}
