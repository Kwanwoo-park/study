package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.SecurityConfig;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.member.Member;
import spring.study.service.BoardService;
import spring.study.service.MemberService;
import spring.study.service.UserService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Controller
@Slf4j
public class MemberController {
    private final MemberService memberService;
    private final UserService userService;
    private final BoardService boardService;
    private Member member;

    @GetMapping("/login")
    public String login(Model model,
                               @RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "exception", required = false) String exception) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        return "/member/login";
    }

    @GetMapping("/logout")
    public String logout(Model model){
        if (member != null)
            model.addAttribute("member", member);
        else
            return "redirect:/login";

        return "/member/logout";
    }

    @GetMapping("/register")
    public String register() {
        return "/member/register";
    }

    @GetMapping("/detail")
    public String detail(Model model){
        if (member == null) return "redirect:/login?error=true&exception=Not Found account";

        model.addAttribute("member", member);

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

    @PostMapping("/login/action")
    public String loginAction(MemberRequestDto dto, HttpServletRequest request) throws Exception {
        try {
            member = (Member) memberService.loadUserByUsername(dto.getEmail());

            if (new BCryptPasswordEncoder().matches(dto.getPassword(), member.getPassword())){
                memberService.updateMemberLastLogin(member.getEmail(), LocalDateTime.now());
                HttpSession session = request.getSession();
                session.setAttribute("member", member);
            }
            else {
                return "redirect:/login?error=true&exception=Invalid Email or Password";
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "redirect:/board/list";
    }

    @PostMapping("/logout/action")
    public String logoutAction() {
        member = null;

        return "redirect:/login";
    }

    @PostMapping("/register/action")
    public String registerAction(MemberRequestDto memberRequestDto) throws Exception {
        try {
            MemberResponseDto memberResponseDto = userService.createUser(memberRequestDto);

            if (memberResponseDto == null) {
                throw new Exception("#이미 존재 하는 이메일 입니다.");
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
        member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());
        if (member == null) return "redirect:/find?error=true&exception=Not Found account";

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
}
