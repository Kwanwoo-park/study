package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class ChatUserStatusApiController {
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/{email}/check")
    public ResponseEntity<String> getStatus(@PathVariable String email, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("offline");

        String status = redisTemplate.opsForValue().get("user:" + email + ":status");
        return ResponseEntity.ok(status != null ? status : "offline");
    }
}
