package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.alert.AlertMessage;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.follow.Follow;
import spring.study.entity.member.Member;
import spring.study.entity.role.Role;
import spring.study.service.FollowService;
import spring.study.service.MemberService;
import spring.study.service.UserService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@Slf4j
public class MemberController {
    private final MemberService memberService;
    private final UserService userService;
    private final FollowService followService;
    private Member member;
    private Member search_member;
    private boolean status;
    private HashMap<String, Object> member_search;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception,
                        HttpServletRequest request) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

        HttpSession session = request.getSession(false);

        if (session != null) {
            member = (Member) session.getAttribute("member");
            return "redirect:/board/list";
        }

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
    public String detail(Model model, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if (session != null) {
            member = (Member) session.getAttribute("member");

            model.addAttribute("member", member);
            model.addAttribute("follower", followService.countFollowing(member.getId()));
            model.addAttribute("following", followService.countFollower(member.getId()));
        }
        else {
            return "redirect:/login?error=true&exception=Not Found account";
        }

        return "/member/detail";
    }

    @GetMapping("/find")
    public String find() {
        return "/member/find";
    }

    @GetMapping("/updatePassword")
    public String updatePassword(Model model) throws Exception {
        try {
            model.addAttribute("email", member.getEmail());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return "/member/updatePassword";
    }

    @GetMapping("/withdrawal")
    public String withdrawal(Model model) {
        if (member == null) return "redirect:/login?error=true&exception=Not Found account";

        model.addAttribute("member", member);

        return "/member/withdrawal";
    }

    @GetMapping("/member_find")
    public String memberFind(Model model) {
        model.addAttribute("member", member_search);

        return "/member/member_find";
    }

    @GetMapping("/member_detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto) {
        status = false;
        search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        List<Follow> follower = followService.findFollower(member.getId());

        model.addAttribute("member", search_member);
        model.addAttribute("follower", followService.countFollowing(search_member.getId()));
        model.addAttribute("following", followService.countFollower(search_member.getId()));

        for (Follow f : follower) {
            if (f.getFollowing().equals(search_member.getId())) {
                status = true;
                model.addAttribute("status", true);
                break;
            }
        }

        if (!status)
            model.addAttribute("status", false);

        return "/member/member_detail";
    }

    @PostMapping("/login/action")
    public String loginAction(MemberRequestDto dto, HttpServletRequest request, Model model) throws Exception {
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

        AlertMessage message;

        if (member.getRole() == Role.ADMIN) {
            message = new AlertMessage(member.getName() + " 관리자님 환영합니다.", "/admin/administrator", RequestMethod.GET, null);
        }
        else {
            message = new AlertMessage(member.getName() + "님 환영합니다.", "/board/list", RequestMethod.GET, null);
        }

        return message.showMessageAndRedirect(model);
    }

    @PostMapping("/logout/action")
    public String logoutAction(HttpServletRequest request) {
        member = null;

        HttpSession session = request.getSession();
        session.invalidate();

        return "redirect:/login";
    }

    @PostMapping("/register/action")
    public String registerAction(MemberRequestDto memberRequestDto, Model model) throws Exception {
        AlertMessage message;
        try {
            MemberResponseDto memberResponseDto = userService.createUser(memberRequestDto);

            if (memberResponseDto == null) {
                message = new AlertMessage("이미 존재하는 이메일입니다.", "/register", RequestMethod.GET, null);
                return message.showMessageAndRedirect(model);
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        message = new AlertMessage(memberRequestDto.getName() + "님 회원가입이 완료되었습니다.", "/login", RequestMethod.GET, null);
        return message.showMessageAndRedirect(model);
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
    public String updatePasswordAction(MemberRequestDto memberUpdateDto, Model model) throws Exception {
        AlertMessage message;
        try {
            int result = memberService.updateMemberPassword(member.getEmail(), memberUpdateDto.getPassword());

            if (result < 0) {
                message = new AlertMessage("변경에 실패했습니다.\n다시 한 번 시도해주세요", "/updatePassword", RequestMethod.GET, null);
                return message.showMessageAndRedirect(model);
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        message = new AlertMessage(member.getName() + "님 비밀번호 변경에 성공했습니다.", "/login", RequestMethod.GET, null);
        return message.showMessageAndRedirect(model);
    }

    @PostMapping("/withdrawal/action")
    public String withdrawalAction() {
        memberService.deleteById(member.getId());

        member = null;

        return "redirect:/login";
    }

    @PostMapping("/member_find/action")
    public String memberFindAction(MemberRequestDto memberRequestDto) {
        member_search = memberService.findName(memberRequestDto.getName());

        return "redirect:/member_find";
    }

    @PostMapping("/member_detail/action")
    public String memberDetailAction(HttpServletRequest request) {
        if (!status) {
            FollowRequestDto followRequestDto = new FollowRequestDto();

            followRequestDto.setFollowing(search_member.getId());
            followRequestDto.setFollower(member.getId());
            followRequestDto.setName(member.getName());

            followService.save(followRequestDto);
        }
        else
            followService.deleteFollow(member.getId());

        return "redirect:/member_detail?email="+search_member.getEmail();
    }
}
