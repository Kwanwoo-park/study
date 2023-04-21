package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.dto.member.MemberRequestDto;
import spring.study.service.MemberService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Controller
public class MemberController {
    private final MemberService memberService;

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

    @PostMapping("/login/action")
    public String loginAction(Model model, MemberRequestDto memberRequestDto) throws Exception {
        try {
            memberService.updateMemberLastLogin(memberRequestDto.getEmail(), LocalDateTime.now());
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "redirect:/board/list";
    }

    @PostMapping("/register/action")
    public String registerAction(Model model, MemberRequestDto memberRequestDto) throws Exception {
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
}
