package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletResponse;
import spring.study.admin.facade.AdminViewFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.member.dto.MemberRequestDto;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminViewController {
    private final AdminViewFacade facade;

    @GetMapping("/access-denied")
    public String accessDenied() {
        return facade.accessDenied();
    }

    @GetMapping("/administrator")
    public String admin(HttpServletRequest request){
        return facade.admin(request);
    }

    @GetMapping("/memberCheck")
    public String member_check(Model model, HttpServletRequest request, @RequestParam(required = false, defaultValue = "0") Integer page, @RequestParam(required = false, defaultValue = "5") Integer size){
        return facade.member_check(model, request, page, size);
    }

    @GetMapping("/member/detail")
    public String memberDetail(Model model, MemberRequestDto requestDto, HttpServletRequest request) {
        return facade.memberDetail(model, requestDto, request);
    }

    @GetMapping("/forbidden/word/list")
    public String forbiddenWordList(Model model, HttpServletRequest request) {
        return facade.forbiddenWordList(model, request);
    }

    @GetMapping("/forbidden/word/apply")
    public String forbiddenWordAppplyList(Model model, HttpServletRequest request) {
        return facade.forbiddenWordAppplyList(model, request);
    }

    @GetMapping("/report")
    public String reportList(Model model, HttpServletRequest request) {
        return facade.reportList(model, request);
    }

    @GetMapping("/report/process")
    public String reportProcessList(HttpServletRequest request) {
        return facade.reportProcessList(request);
    }

    @GetMapping("/report/history")
    public String reportHistory(HttpServletRequest request) {
        return facade.reportHistory(request);
    }

    @GetMapping("/appeal")
    public String appealList(Model model, HttpServletRequest request) {
        return facade.appealList(model, request);
    }

    @GetMapping("/files")
    public String page() {
        return facade.page();
    }

    @GetMapping("/github")
    public String pageGithub(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "admin/github";
    }

    @GetMapping("/event-logs")
    public String pageEventLog(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "admin/event_logs";
    }

    @GetMapping("/kafka")
    public String pageKafka(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "admin/kafka_operations";
    }
}
