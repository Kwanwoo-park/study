package spring.study.diary.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.SessionManager;
import spring.study.diary.facade.DiaryFacade;
import spring.study.member.entity.Member;

@Controller
@RequiredArgsConstructor
@RequestMapping("/diary")
public class DiaryViewController {
    private static final int DIARY_LOAD_SIZE = 100;

    private final DiaryFacade diaryFacade;
    private final SessionManager sessionManager;

    @GetMapping("/write")
    public String write(
            @RequestParam(required = false) Long id,
            Model model,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/diary/write";
        }

        addMemberModel(model, member);
        if (id != null) {
            model.addAttribute("diary", diaryFacade.findById(id, member));
        }

        return "diary/write";
    }

    @GetMapping({"", "/list"})
    public String list(
            Model model,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return "redirect:/member/login?error=true&exception=Not Found&url=/diary/list";
        }

        addMemberModel(model, member);
        long count = diaryFacade.count(member);
        model.addAttribute("diaries", diaryFacade.findByMember(member, 0, DIARY_LOAD_SIZE));
        model.addAttribute("count", count);
        model.addAttribute("hasNext", count > DIARY_LOAD_SIZE);

        return "diary/list";
    }

    private void addMemberModel(Model model, Member member) {
        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
    }
}
