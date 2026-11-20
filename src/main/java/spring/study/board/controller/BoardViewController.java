package spring.study.board.controller;

import spring.study.board.facade.BoardViewFacade;
import spring.study.board.facade.BoardBookmarkFacade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
@Slf4j
public class BoardViewController {
    private final BoardViewFacade facade;
    private final BoardBookmarkFacade bookmarkFacade;
    private final JwtManager jwtManager;

    @GetMapping("/bookmarks")
    public String bookmarks(@RequestParam(defaultValue = "0") int page, Model model, HttpServletRequest request,
                            HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?url=/board/bookmarks";
        response.setHeader("Cache-Control", "no-store");
        return bookmarkFacade.view(model, member, page);
    }

    @GetMapping("/all")
    public String getBoardListPage(Model model, @RequestParam(required = false, defaultValue = "0") Integer page, @RequestParam(required = false, defaultValue = "5") Integer size, HttpServletRequest request) {
        return facade.getBoardListPage(model, page, size, request);
    }

    @GetMapping("/main")
    public String mainPage(Model model, HttpServletRequest request) {
        return facade.mainPage(model, request);
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpServletRequest request){
        return facade.getBoardWritePage(model, request);
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpServletRequest request) {
        return facade.getBoardViewPage(model, boardRequestDto, request);
    }
}
