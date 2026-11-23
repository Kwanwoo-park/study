package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.member.facade.RegistrationEmailFacade;

@Controller
@RequiredArgsConstructor
public class RegistrationEmailController {
    private final RegistrationEmailFacade facade;

    @GetMapping("/member/register")
    public String registration(Model model, HttpServletRequest request, HttpServletResponse response) {
        return facade.registration(model, request, response);
    }

    @GetMapping("/member/register/verify")
    public String verification(Model model, HttpServletRequest request, HttpServletResponse response) {
        return facade.verification(model, request, response);
    }

    @PostMapping("/member/register/verify/send")
    public String send(@RequestParam String email, Model model, HttpServletRequest request, HttpServletResponse response) {
        return facade.send(email, model, request, response);
    }

    @PostMapping("/member/register/verify")
    public String verify(@RequestParam String code, Model model, HttpServletRequest request, HttpServletResponse response) {
        return facade.verify(code, model, request, response);
    }

    @PostMapping("/member/register/verify/restart")
    public String restart(HttpServletRequest request) {
        return facade.restart(request);
    }
}
