package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.Member;
import spring.study.service.FollowService;
import spring.study.service.MemberService;
import spring.study.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final UserService userService;
    private final FollowService followService;
    private Member member;

    @PostMapping("/register/action")
    public MemberResponseDto registerAction(@RequestBody MemberRequestDto memberRequestDto, Model model) throws Exception {
        MemberResponseDto memberResponseDto;
        try {
             memberResponseDto= userService.createUser(memberRequestDto);

            if (memberResponseDto == null) {
                return null;
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return memberResponseDto;
    }

    @PatchMapping("/detail/action")
    public int detailAction(@RequestParam MultipartFile file, HttpSession session) throws IOException {
        String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

        member = (Member) session.getAttribute("member");

        File f = new File(fileDir + file.getOriginalFilename());

        if (!f.exists()) {
            file.transferTo(new File(fileDir + file.getOriginalFilename()));
        }

        int result = memberService.updateMemberProfile(member.getEmail(), file.getOriginalFilename());

        member.setProfile(file.getOriginalFilename());

        session.setAttribute("member", member);

        return result;
    }

    @GetMapping("/find/{email}/action")
    @ResponseBody
    public Member findAction(@PathVariable String email,
                             HttpSession session) {
        member = memberService.findMember(email);

        session.setAttribute("member", member);

        return member;
    }

    @PatchMapping("/updatePassword/action")
    public int updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto,
                                    HttpSession session) throws Exception {
        int result;

        try {
            result = memberService.updateMemberPassword(memberUpdateDto.getEmail(), memberUpdateDto.getPassword());

            member = null;
            session.invalidate();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/withdrawal/action")
    public void withdrawalAction(HttpSession session) {
        member = (Member) session.getAttribute("member");

        memberService.deleteById(member.getId());

        member = null;

        session.invalidate();
    }


    @GetMapping("/search/{name}/action")
    public boolean memberFindAction(@PathVariable String name, HttpSession session) {
        HashMap<String, Object> member_search = memberService.findName(name);

        session.setAttribute("member_search", member_search);

        return member_search.isEmpty();
    }
}
