package spring.study.admin.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import spring.study.common.service.JwtManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminViewFacade {
    private final MemberService memberService;
    private final JwtManager jwtManager;
    private final ForbiddenService forbiddenService;

    public String accessDenied() {
        return "error/404";
    }

    public String admin(HttpServletRequest request){
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/administrator";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "admin/administrator";
    }

    public String member_check(Model model, HttpServletRequest request, Integer page, Integer size){
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/memberCheck";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findAll(page, size));

        return "admin/member_check";
    }

    public String memberDetail(Model model, MemberRequestDto requestDto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/member/detail";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findMember(requestDto.getEmail()));

        return "admin/update_member";
    }

    public String forbiddenWordList(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/forbidden/word/list";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("list", forbiddenService.findAll());

        return "admin/forbidden_word_list";
    }

    public String forbiddenWordAppplyList(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/forbidden/word/apply";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("list", forbiddenService.findByStatusNot(Status.APPROVAL));

        return "admin/forbidden_word_apply";
    }

    public String reportList(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/report";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "admin/report_apply";
    }

    public String reportProcessList(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/report/process";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "admin/report_process";
    }

    public String reportHistory(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/report/history";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "admin/report_history";
    }

    public String appealList(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/admin/appeal";

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "admin/appeal_list";
    }

    public String page() {
        return "admin/files";
    }
}
