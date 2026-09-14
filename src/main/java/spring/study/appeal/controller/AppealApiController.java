package spring.study.appeal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealVerificationConfirmRequest;
import spring.study.appeal.dto.AppealVerificationSendRequest;
import spring.study.appeal.facade.AppealFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appeal")
public class AppealApiController {
    private final AppealFacade appealFacade;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping("/context")
    public ResponseEntity<?> context(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return appealFacade.context(member);
    }

    @PostMapping("/verification/send")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody AppealVerificationSendRequest verificationRequest,
                                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "이메일을 확인해주세요");

        return appealFacade.sendVerificationCode(verificationRequest.email());
    }

    @PostMapping("/verification/verify")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody AppealVerificationConfirmRequest verificationRequest,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "인증번호를 확인해주세요");

        return appealFacade.verifyEmail(verificationRequest);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AppealRequestDto requestDto,
                                    BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "상소문 입력 내용을 확인해주세요");

        return appealFacade.create(requestDto, jwtManager.getLoginMember(request));
    }
}
