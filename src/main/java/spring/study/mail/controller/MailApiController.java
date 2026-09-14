package spring.study.mail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.mail.facade.MailFacade;

import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mail")
public class MailApiController {
    private final MailFacade mailFacade;

    @PostMapping("/confirm")
    public ResponseEntity<HashMap<String, String>> mailConfirm(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        return mailFacade.confirm(memberRequestDto.getEmail());
    }
}
