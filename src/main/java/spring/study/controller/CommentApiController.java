package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.Member;
import spring.study.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentApiController {
    private final CommentService commentService;

    @PostMapping("/{bid}/action")
    public Long commentAction(@PathVariable Long bid,
                              @RequestBody CommentRequestDto commentRequestDto,
                              HttpSession session) {

        Member member = (Member) session.getAttribute("member");



        return null;
    }
}
