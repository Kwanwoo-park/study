package spring.study.todo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.todo.dto.TodoCompletionRequestDto;
import spring.study.todo.dto.TodoRequestDto;
import spring.study.todo.facade.TodoFacade;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todo")
public class TodoApiController {
    private final TodoFacade todoFacade;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean completed,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return todoFacade.list(member, completed, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return todoFacade.detail(id, member);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TodoRequestDto dto, BindingResult errors, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (errors.hasErrors()) return commonFacade.validationFailure(errors, "할 일을 확인해주세요");

        return todoFacade.create(dto.content(), member);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TodoRequestDto dto,
                                    BindingResult errors, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (errors.hasErrors()) return commonFacade.validationFailure(errors, "할 일을 확인해주세요");

        return todoFacade.update(id, dto.content(), member);
    }

    @PatchMapping("/{id}/completion")
    public ResponseEntity<?> updateCompletion(@PathVariable Long id, @Valid @RequestBody TodoCompletionRequestDto dto,
                                              BindingResult errors, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (errors.hasErrors()) return commonFacade.validationFailure(errors, "완료 여부를 확인해주세요");

        return todoFacade.updateCompletion(id, dto.completed(), member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return todoFacade.delete(id, member);
    }
}
