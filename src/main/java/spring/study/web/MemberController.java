package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.member.Member;
import spring.study.service.BoardService;
import spring.study.service.MemberService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Controller
public class MemberController {
    private final MemberService memberService;
    private final BoardService boardService;
    private Member member;

    @GetMapping("/login")
    public String getLoginPage(Model model,
                               @RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "exception", required = false) String exception) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        return "/member/login";
    }

    @GetMapping("/register")
    public String register() {
        return "/member/register";
    }

    @GetMapping("/detail")
    public String detail(Model model) throws Exception{
        try {
            model.addAttribute("member", member);
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "/member/detail";
    }

    @GetMapping("/find")
    public String find() {
        return "/member/find";
    }

    @GetMapping("/updatePassword")
    public String updatePassword(Model model) throws Exception{
        try {
            model.addAttribute("email", member.getEmail());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "/member/updatePassword";
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

    @PostMapping("/login/action")
    public String loginAction(MemberRequestDto dto) throws Exception {
        try {
            member = memberService.loadUserByUsername(dto.getEmail());
            memberService.updateMemberLastLogin(member.getEmail(), LocalDateTime.now());
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "redirect:/board/list";
    }

    @PostMapping("/register/action")
    public String registerAction(MemberRequestDto memberRequestDto) throws Exception {
        try {
            Long result = memberService.save(memberRequestDto);

            if (result < 0) {
                throw new Exception("#Exception memberRegisterAction");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/login";
    }

    @PostMapping("/detail/action")
    public String detailAction() {

        return "redirect:/updatePassword";
    }

    @PostMapping("/find/action")
    public String findAction(MemberRequestDto memberRequestDto) {
        member = memberService.loadUserByUsername(memberRequestDto.getEmail());

        return "redirect:/updatePassword";
    }

    @PostMapping("/updatePassword/action")
    public String updatePasswordAction(MemberRequestDto memberUpdateDto) throws Exception {
        try {
            int result = memberService.updateMemberPassword(member.getEmail(), memberUpdateDto.getPassword());

            if (result < 0) {
                throw new Exception("#Exception memberRegisterAction");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/login";
    }

    @PostMapping("/board/write/action")
    public String boardWriteAction(BoardRequestDto boardRequestDto) throws Exception {
        try {
            boardRequestDto.setRegisterId(member.getName());
            Long result = boardService.save(boardRequestDto);

            if (result < 0) {
                throw new Exception("#Exception boardWriteAction!");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }
}
