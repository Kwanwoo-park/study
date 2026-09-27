package spring.study.member.controller;

import spring.study.member.facade.MemberViewFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
@Slf4j
public class MemberViewController {
    private final MemberViewFacade facade;

    @GetMapping("/login")
    public String login(Model model, @RequestParam(value = "error", required = false) String error, @RequestParam(value = "exception", required = false) String exception, @RequestParam(value = "url", required = false) String url, HttpServletRequest request) {
        return facade.login(model, error, exception, url, request);
    }

    @GetMapping("/register")
    public String register() {
        return facade.register();
    }

    @GetMapping("/detail")
    public String detail(@RequestParam String email, Model model, HttpServletRequest request){
        return facade.detail(email, model, request);
    }

    @GetMapping("/find")
    public String find() {
        return facade.find();
    }

    @GetMapping("/findByEmail")
    public String findByEmail() {
        return facade.findByEmail();
    }

    @GetMapping("/findByInfo")
    public String findByInfo() {
        return facade.findByInfo();
    }

    @GetMapping("/updatePassword")
    public String updatePassword(Model model, HttpServletRequest request) {
        return facade.updatePassword(model, request);
    }

    @GetMapping("/updatePhone")
    public String updatePhone(Model model, HttpServletRequest request) {
        return facade.updatePhone(model, request);
    }

    @GetMapping("/updatePassword/{email}")
    public String updatePassword(@PathVariable String email, Model model) throws Exception {
        return facade.updatePassword(email, model);
    }

    @GetMapping("/withdrawal")
    public String withdrawal(Model model, HttpServletRequest request) {
        return facade.withdrawal(model, request);
    }

    @GetMapping("/search")
    public String memberFind(Model model, HttpServletRequest request) {
        return facade.memberFind(model, request);
    }

    @GetMapping("/search/detail")
    public String memberDetail(Model model,
                               MemberRequestDto memberRequestDto,
                               @RequestParam(value = "source", required = false) String source,
                               HttpServletRequest request) {
        return facade.memberDetail(model, memberRequestDto, source, request);
    }
}
