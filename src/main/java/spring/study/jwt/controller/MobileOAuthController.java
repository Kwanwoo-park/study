package spring.study.jwt.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.ClientIpResolver;
import spring.study.jwt.dto.MobileOAuthExchangeRequest;
import spring.study.jwt.facade.MobileOAuthFacade;

@RestController
@RequestMapping("/api/mobile/auth/oauth")
@RequiredArgsConstructor
public class MobileOAuthController {
    private final MobileOAuthFacade mobileOAuthFacade;

    @GetMapping("/{provider}")
    public ResponseEntity<?> start(@PathVariable String provider, HttpServletResponse response) {
        return mobileOAuthFacade.start(provider, response);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody MobileOAuthExchangeRequest request, HttpServletRequest servletRequest) {
        return mobileOAuthFacade.exchange(request, ClientIpResolver.resolve(servletRequest));
    }
}
