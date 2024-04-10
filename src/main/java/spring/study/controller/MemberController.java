package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.alert.AlertMessage;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.Board;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.BoardService;
import spring.study.service.FollowService;
import spring.study.service.MemberService;
import spring.study.service.UserService;

import java.io.File;
import java.io.IOException;
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
    private final BoardService boardService;
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
            if (member == null)
                session.invalidate();
            else {
                if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
                return "redirect:/board/list";
            }
        }

        return "/member/login";
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
            List<Board> list = boardService.findEmail(member.getEmail());

            model.addAttribute("member", member);
            model.addAttribute("follower", followService.countFollowing(member.getId()));
            model.addAttribute("following", followService.countFollower(member.getId()));
            model.addAttribute("list", list);
        }
        else {
            return "redirect:/login?error=true&exception=Not Found account";
        }

        return "/member/detail";
    }

    @GetMapping("/find")
    public String find(Model model,
                       @RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "exception", required = false) String exception) {

        model.addAttribute("error", error);
        model.addAttribute("exception", exception);

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

        model.addAttribute("name", member.getName());

        return "/member/withdrawal";
    }

    @GetMapping("/member/find")
    public String memberFind(Model model) {
        model.addAttribute("member", member_search);

        return "/member/member_find";
    }

    @GetMapping("/member/detail")
    public String memberDetail(Model model, MemberRequestDto memberRequestDto,
                               HttpServletRequest request) {
        status = false;
        search_member = (Member) memberService.loadUserByUsername(memberRequestDto.getEmail());

        HttpSession session = request.getSession();
        if (session == null)
            return "redirect:/login?error=true&exception=Not Found account";

        member = (Member) session.getAttribute("member");

        List<Follow> follower = followService.findFollower(member.getId());

        model.addAttribute("member", search_member);
        model.addAttribute("equal_check", search_member.getEmail().equals(member.getEmail()));
        model.addAttribute("follower", followService.countFollowing(search_member.getId()));
        model.addAttribute("following", followService.countFollower(search_member.getId()));
        model.addAttribute("list", boardService.findEmail(search_member.getEmail()));

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

    @GetMapping("/login/action")
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
            return "redirect:/login?error=true&exception=Invalid Email or Password";
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

    @GetMapping("/logout/action")
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

    @PatchMapping("/detail/action")
    @ResponseBody
    public int detailAction(@RequestParam MultipartFile file, HttpServletRequest request) throws IOException {
       String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

       File f = new File(fileDir + file.getOriginalFilename());

        if (!f.exists()) {
            file.transferTo(new File(fileDir + file.getOriginalFilename()));
        }

        int result = memberService.updateMemberProfile(member.getEmail(), file.getOriginalFilename());

        member.setProfile(file.getOriginalFilename());

        HttpSession session = request.getSession();
        session.setAttribute("member", member);

        return result;
    }

    @GetMapping("/find/action")
    public String findAction(MemberRequestDto memberRequestDto) {
        member = memberService.findMember(memberRequestDto.getEmail());
        if (member == null) return "redirect:/find?error=true&exception=Not Found account";

        return "redirect:/updatePassword";
    }

    @PatchMapping("/updatePassword/action")
    @ResponseBody
    public int updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto,
                                     HttpServletRequest request) throws Exception {
        int result;

        try {
            result = memberService.updateMemberPassword(member.getEmail(), memberUpdateDto.getPassword());

            member = null;
            HttpSession session = request.getSession();
            session.invalidate();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/withdrawal/action")
    @ResponseBody
    public void withdrawalAction(HttpServletRequest request) {
        memberService.deleteById(member.getId());

        member = null;

        HttpSession session = request.getSession();
        session.invalidate();
    }

    @GetMapping("/member/find/{name}/action")
    @ResponseBody
    public boolean memberFindAction(@PathVariable String name) {
        member_search = memberService.findName(name);

        return member_search.isEmpty();
    }

    @PatchMapping("/member/detail/action")
    @ResponseBody
    public Long memberDetailAction() {
        if (!status) {
            FollowRequestDto followRequestDto = new FollowRequestDto();

            followRequestDto.setFollower(member.getId());
            followRequestDto.setFollower_name(member.getName());
            followRequestDto.setFollower_email(member.getEmail());
            followRequestDto.setFollowing(search_member.getId());
            followRequestDto.setFollowing_name(search_member.getName());
            followRequestDto.setFollowing_email(search_member.getEmail());

            return followService.save(followRequestDto);
        }
        else
            return followService.deleteFollow(member.getId(), search_member.getId());
    }
}
