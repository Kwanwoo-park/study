package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.admin.facade.IpBlockFacade;
import spring.study.member.entity.Member;

@Controller
@RequestMapping("/admin/ip-blocks")
@RequiredArgsConstructor
public class IpBlockController {
    private final IpBlockFacade facade;

    @GetMapping
    public String list(Model model, HttpServletResponse response) {
        return facade.list(model, response);
    }

    @PostMapping
    public String block(@RequestParam String ipAddress, @AuthenticationPrincipal Member admin,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        return facade.block(ipAddress, admin, request, response, model);
    }

    @PostMapping("/email")
    public String blockEmail(@RequestParam String email, @AuthenticationPrincipal Member admin,
                             HttpServletResponse response, Model model) {
        return facade.blockEmail(email, admin, response, model);
    }
}
