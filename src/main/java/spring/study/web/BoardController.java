package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private Member member;
    private final MemberService memberService;

    @GetMapping("/board/list/{memberId}")
    public String getBoardListPage(Model model,
                                   @PathVariable String memberId,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        member = memberService.findById(Long.parseLong(memberId)).get();
        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/list";
    }

    @GetMapping("/board/write")
    public String getBoardWritePage(Model model) throws Exception {
        try {
            model.addAttribute("name", member.getName());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "/board/write";
    }

    @GetMapping("/board/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            if (boardRequestDto.getId() != null) {
                model.addAttribute("info", boardService.findById(boardRequestDto.getId()));
                model.addAttribute("member", memberService.loadUserByUsername(member.getEmail()));
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/view";
    }

    @PostMapping("/board/write/action")
    public String boardWriteAction(BoardRequestDto boardRequestDto) throws Exception {
        try {
            boardRequestDto.setRegisterId(member.getName());
            boardRequestDto.setRegisterEmail(member.getEmail());
            Long result = boardService.save(boardRequestDto);

            if (result < 0) {
                throw new Exception("#Exception boardWriteAction!");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list/" + member.getId();
    }

    @PostMapping("/board/view/action")
    public String boardViewAction(BoardRequestDto boardRequestDto) throws Exception {
        try {
            boardService.updateBoard(boardRequestDto);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list/"+member.getId();
    }

    @PostMapping("/board/view/delete")
    public String boardViewDeleteAction(@RequestParam() Long id) throws Exception {
        try {
            boardService.deleteById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list/"+member.getId();
    }

    @PostMapping("/board/delete")
    public String boardDeleteAction(@RequestParam() Long[] deleteId) throws Exception {
        try {
            boardService.deleteAll(deleteId);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list/"+member.getId();
    }
}
