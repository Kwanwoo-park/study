package spring.study.appeal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.dto.AppealVerificationConfirmRequest;
import spring.study.appeal.dto.AppealVerificationSendRequest;
import spring.study.appeal.service.AppealService;
import spring.study.appeal.service.AppealVerificationService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appeal")
public class AppealApiController {
    private final AppealService appealService;
    private final AppealVerificationService appealVerificationService;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping("/context")
    public ResponseEntity<?> context(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) {
            return commonFacade.unauthorized();
        }
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "memberName", member.getName() == null ? "" : member.getName(),
                "memberEmail", member.getEmail(),
                "sanctions", appealService.findSanctions(member),
                "appeals", appealService.findByMember(member)
        ));
    }

    @PostMapping("/verification/send")
    public ResponseEntity<?> sendVerificationCode(
            @Valid @RequestBody AppealVerificationSendRequest verificationRequest,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) return validationFailure(bindingResult, "이메일을 확인해주세요");

        try {
            appealVerificationService.sendCode(verificationRequest.email());
            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "expiresInSeconds", 300,
                    "message", "가입된 이메일이라면 인증번호가 발송됩니다. 인증번호는 5분 동안 유효합니다"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "인증번호를 발송할 수 없습니다");
        }
    }

    @PostMapping("/verification/verify")
    public ResponseEntity<?> verifyEmail(
            @Valid @RequestBody AppealVerificationConfirmRequest verificationRequest,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) return validationFailure(bindingResult, "인증번호를 확인해주세요");

        try {
            String verificationToken = appealVerificationService.verifyCode(
                    verificationRequest.email(), verificationRequest.code());
            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "verificationToken", verificationToken,
                    "expiresInSeconds", 300,
                    "message", "이메일 인증이 완료되었습니다. 5분 이내에 상소문을 제출해주세요"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "이메일 인증에 실패했습니다");
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AppealRequestDto requestDto,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().isEmpty()
                    ? "상소문 입력 내용을 확인해주세요"
                    : bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of(
                    "result", -10L,
                    "message", message == null ? "상소문 입력 내용을 확인해주세요" : message
            ));
        }
        try {
            Member member = jwtManager.getLoginMember(request);
            AppealResponseDto appeal = appealService.create(requestDto, member);
            return ResponseEntity.ok(Map.of(
                    "result", appeal.getId(),
                    "appeal", appeal,
                    "message", "상소문이 접수되었습니다"
            ));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                    "result", -10L,
                    "message", exception.getReason() == null ? "상소문을 접수할 수 없습니다" : exception.getReason()
            ));
        }
    }

    private ResponseEntity<?> validationFailure(BindingResult bindingResult, String fallbackMessage) {
        String message = bindingResult.getFieldErrors().isEmpty()
                ? fallbackMessage
                : bindingResult.getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "result", -10L,
                "message", message == null ? fallbackMessage : message
        ));
    }

    private ResponseEntity<?> statusFailure(ResponseStatusException exception, String fallbackMessage) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "result", -10L,
                "message", exception.getReason() == null ? fallbackMessage : exception.getReason()
        ));
    }
}
