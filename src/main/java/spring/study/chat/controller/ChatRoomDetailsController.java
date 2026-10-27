package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import spring.study.chat.dto.ChatRoomDetailsResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.facade.ChatRoomFacade;
import spring.study.common.service.JwtManager;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms/{roomId}")
public class ChatRoomDetailsController {
    private final JwtManager jwtManager;
    private final ChatRoomFacade chatRoomFacade;

    @GetMapping("/details")
    public ResponseEntity<ChatRoomDetailsResponse> details(@PathVariable String roomId, HttpServletRequest request) {
        return chatRoomFacade.details(roomId, jwtManager.getLoginMember(request));
    }

    @GetMapping("/images")
    public ResponseEntity<ChatRoomImagesResponse> images(@PathVariable String roomId,
            @RequestParam(required = false) Long cursor, HttpServletRequest request) {
        return chatRoomFacade.images(roomId, jwtManager.getLoginMember(request), cursor);
    }
}
