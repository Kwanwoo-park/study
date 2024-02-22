package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.entity.chat.ChatRoom;
import spring.study.service.ChatService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {
   private final ChatService chatService;

   @RequestMapping("/chat/chatList")
    public String chatList(Model model) {
       List<ChatRoom> roomList = chatService.findAll();
       model.addAttribute("roomList", roomList);
       return "chat/chatList";
   }

   @PostMapping("/chat/createRoom")
    public String createRoom(Model model, @RequestParam String name, String username) {
       ChatRoom room = chatService.createRoom(name);
       model.addAttribute("room", room);
       model.addAttribute("username", username);
       return "chat/chatRoom";
   }

   @GetMapping("/chat/chatRoom")
    public String chatRoom(Model model, @RequestParam String roomId) {
       ChatRoom room = chatService.findRoomId(roomId);
       model.addAttribute("room", room);
       return "chat/chatRoom";
   }
}
