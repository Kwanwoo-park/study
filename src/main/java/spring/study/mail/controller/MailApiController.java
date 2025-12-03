package spring.study.mail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.mail.service.RegisterMail;

import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mail")
public class MailApiController {
    private final RegisterMail registerMail;

    @PostMapping("/confirm")
    public ResponseEntity<HashMap<String, String>> mailConfirm(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        try {
            String code = registerMail.sendSimpleMessage(memberRequestDto.getEmail());
            map.put("status", "ok");
            map.put("code", code);
        } catch (Exception e) {
            map.put("status", "fail");
        }

        return ResponseEntity.ok(map);
    }
}
