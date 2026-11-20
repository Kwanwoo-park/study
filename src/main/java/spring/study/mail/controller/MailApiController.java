package spring.study.mail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.mail.facade.MailFacade;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mail")
public class MailApiController {
    private final MailFacade mailFacade;

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> mailConfirm(@RequestBody MemberRequestDto memberRequestDto) {
        return mailFacade.confirm(memberRequestDto.getEmail());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody MemberRequestDto request) {
        return mailFacade.verify(request.getEmail(), request.getCode());
    }
}
