package spring.study.chat.controller;

import spring.study.chat.facade.ChatPageFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {
    private final ChatPageFacade facade;

    @GetMapping("/chatList")
    public String chatList(Model model, HttpServletRequest request) {
        return facade.chatList(model, request);
    }

    @GetMapping("/chatRoom")
    public String chatRoom(@RequestParam String roomId, Model model, HttpServletRequest request) {
        return facade.chatRoom(roomId, model, request);
    }


}
