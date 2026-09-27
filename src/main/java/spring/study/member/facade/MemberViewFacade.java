package spring.study.member.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import spring.study.account.service.AccountService;
import spring.study.common.service.JwtManager;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.board.service.BoardService;
import spring.study.follow.service.FollowService;
import spring.study.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberViewFacade {
    private final MemberService memberService;
    private final BoardService boardService;
    private final FollowService followService;
    private final JwtManager jwtManager;
    private final AccountService accountService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;

    public String login(Model model, String error, String exception, String url, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);

        if (member != null) {
            if (member.getRole() == Role.USER) return "redirect:/board/main";
            else if (member.getRole() == Role.ADMIN) return "redirect:/admin/administrator";
        }

        model.addAttribute("error", error);
        model.addAttribute("exception", exception);
        model.addAttribute("url", url);

        return "member/login";
    }

    public String register() {
        return "member/register";
    }

    public String detail(String email, Model model, HttpServletRequest request){
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/detail?email=" + email;

        String memberEmail = member.getEmail();

        if (!email.equals(memberEmail))
            return "redirect:/member/search/detail?email=" + email;

        model.addAttribute("member", member);
        model.addAttribute("board_count", boardService.countByMember(member));
        model.addAttribute("follower_count", followService.countFollowers(member));
        model.addAttribute("following_count", followService.countFollowing(member));

        return "member/detail";
    }

    public String find() {
        return "member/find";
    }

    public String findByEmail() {
        return "member/email_find";
    }

    public String findByInfo() {
        return "member/info_find";
    }

    public String updatePassword(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/updatePassword";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("emailVerificationRequired", true);

        return "member/updatePassword";
    }

    public String updatePhone(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/updatePhone";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("phone", member.getPhone());
        model.addAttribute("birth", member.getBirth());

        return "member/updatePhone";
    }

    public String updatePassword(String email, Model model) throws Exception {
        model.addAttribute("email", email);
        model.addAttribute("emailVerificationRequired", false);

        return "member/updatePassword";
    }

    public String withdrawal(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/withdrawal";

        model.addAttribute("name", member.getName());

        return "member/withdrawal";
    }

    public String memberFind(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/search";

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "member/member_find";
    }

    public String memberDetail(Model model,
                               MemberRequestDto memberRequestDto,
                               String source,
                               HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/member/search/detail?email=" + memberRequestDto.getEmail();

        String memberEmail = member.getEmail();

        if (memberEmail.equals(memberRequestDto.getEmail()))
            return "redirect:/member/detail?email=" + memberEmail;

        Member search_member = memberService.findMember(memberRequestDto.getEmail());
        if (!visibilityAccessPolicy.canViewMember(search_member, member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 회원입니다.");
        }
        boolean includePrivateBoards = followService.existFollow(member, search_member);

        model.addAttribute("member", search_member);
        model.addAttribute("status", includePrivateBoards);
        model.addAttribute("board_count", boardService.countByMember(search_member, includePrivateBoards));
        model.addAttribute("follower_count", followService.countFollowers(search_member));
        model.addAttribute("following_count", followService.countFollowing(search_member));
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());
        model.addAttribute("chatEntry", "chat".equalsIgnoreCase(source));
        model.addAttribute("transferAccounts", accountService.findActiveByMember(search_member));

        return "member/member_detail";
    }
}
