package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.board.service.BoardService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
@Slf4j
public class BoardViewController {
    private final BoardService boardService;

    @GetMapping("/all")
    public String getBoardListPage(Model model,
                                   @RequestParam(required = false, defaultValue = "") String title,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size,
                                   HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getPhone().equals(" ")) {
            return "redirect:/member/updatePhone";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "board/list";
    }

    @GetMapping("/list")
    public String getBoardPage(Model model,
                               @RequestParam(required = false, defaultValue = "0") Integer page,
                               @RequestParam(required = false, defaultValue = "5") Integer size,
                               HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "redirect:/board/all";
    }

    @GetMapping("/main")
    public String mainPage(Model model, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getPhone().equals(" ") || member.getBirth().equals("1900-01-01")) {
            return "redirect:/member/updatePhone";
        }

        try {
            //List<Board> list = boardService.findByMembers(member);
            List<BoardResponseDto> list = boardService.getBoard(null, 10);
            model.addAttribute("resultMap", list);
            model.addAttribute("nextCursor", list.isEmpty() ? null : list.get(list.size()-1).getRegisterTime());
            model.addAttribute("like", member.checkFavorite(list));
            model.addAttribute("profile", member.getProfile());
            model.addAttribute("email", member.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

        return "board/main";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("name", member.getName());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (boardService.existBoard(boardRequestDto.getId())) {
            Board board = boardService.findById(boardRequestDto.getId());
            Member board_member = board.getMember();

            List<Long> id_list = board_member.getBoard().stream().map(Board::getId).toList();

            int size = id_list.size();
            long previous_id = 0L;
            long next_id = 0L;

            if (size > 1) {
                int idx = id_list.indexOf(boardRequestDto.getId());

                if (idx == 0) previous_id = id_list.get(idx+1);
                else if (idx == size-1) next_id = id_list.get(idx-1);
                else {
                    previous_id = id_list.get(idx+1);
                    next_id = id_list.get(idx-1);
                }
            }

            model.addAttribute("board", board);
            model.addAttribute("like", member.checkFavorite(board));
            model.addAttribute("member", member.getEmail());
            model.addAttribute("previous", previous_id);
            model.addAttribute("next", next_id);
        } else {
            return "redirect:/member/detail?email="+member.getEmail();
        }

        return "board/view";
    }
}
