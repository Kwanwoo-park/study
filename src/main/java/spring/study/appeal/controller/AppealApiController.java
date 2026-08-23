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
import spring.study.appeal.service.AppealService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appeal")
public class AppealApiController {
    private final AppealService appealService;
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
}
