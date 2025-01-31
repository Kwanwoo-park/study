package spring.study.controller.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.member.MemberRequestDto;
import spring.study.service.mail.RegisterMail;

import java.util.HashMap;

@RestController
@RequestMapping("/api/mail")
public class MailApiController {
    @Autowired
    RegisterMail registerMail;

    @PostMapping("/confirm")
    public ResponseEntity<HashMap<String, String>> mailConfirm(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        String code = registerMail.sendSimpleMessage(memberRequestDto.getEmail());
        HashMap<String, String> map = new HashMap<>();
        map.put("code", code);

        return ResponseEntity.ok(map);
    }
}
