package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.member.Member;
import spring.study.service.BoardService;
import spring.study.service.MemberService;

@RequiredArgsConstructor
@Controller
public class BoardController {
    private final BoardService boardService;
    private final MemberService memberService;
    private Member member;

    @GetMapping("/board/list")
    public String getBoardListPage(Model model,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/list";
    }

    @GetMapping("/board/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            if (boardRequestDto.getId() != null) {
                model.addAttribute("info", boardService.findById(boardRequestDto.getId()));
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/view";
    }

    @PostMapping("/board/view/action")
    public String boardViewAction(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            boardService.updateBoard(boardRequestDto);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }

    @PostMapping("/board/view/delete")
    public String boardViewDeleteAction(Model model, @RequestParam() Long id) throws Exception {
        try {
            boardService.deleteById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }

    @PostMapping("/board/delete")
    public String boardDeleteAction(Model model, @RequestParam() Long[] deleteId) throws Exception {
        try {
            boardService.deleteAll(deleteId);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }
}
