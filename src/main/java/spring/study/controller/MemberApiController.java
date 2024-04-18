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
import spring.study.dto.follow.FollowResponseDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
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
@RestController
@RequestMapping("/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final UserService userService;
    private final FollowService followService;
    private Member member;
    private HashMap<String, Object> member_search;
    HttpSession session;

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
    public int detailAction(@RequestParam MultipartFile file, HttpServletRequest request) throws IOException {
        String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";
        session = request.getSession();

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

    @PatchMapping("/updatePassword/action")
    public int updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto,
                                    HttpServletRequest request) throws Exception {
        int result;

        try {
            result = memberService.updateMemberPassword(memberUpdateDto.getEmail(), memberUpdateDto.getPassword());

            member = null;
            HttpSession session = request.getSession();
            session.invalidate();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/withdrawal/action")
    public void withdrawalAction(HttpServletRequest request) {
        session = request.getSession();
        member = (Member) session.getAttribute("member");

        memberService.deleteById(member.getId());

        member = null;

        HttpSession session = request.getSession();
        session.invalidate();
    }

    @PatchMapping("/search/detail/action")
    public Long memberDetailAction(@RequestBody FollowRequestDto followRequestDto,
                                   HttpServletRequest request) {

        HttpSession session = request.getSession();
        member = (Member) session.getAttribute("member");

        List<Follow> follower = followService.findFollower(member.getId());
        boolean status = false;

        for (Follow f : follower) {
            if (f.getFollowing().equals(followRequestDto.getFollowing())) {
                status = true;
                break;
            }
        }

        if (!status) {
            followRequestDto.setFollower(member.getId());
            followRequestDto.setFollower_name(member.getName());
            followRequestDto.setFollower_email(member.getEmail());

            return followService.save(followRequestDto);
        }
        else
            return followService.deleteFollow(member.getId(), followRequestDto.getFollowing());
    }
}
