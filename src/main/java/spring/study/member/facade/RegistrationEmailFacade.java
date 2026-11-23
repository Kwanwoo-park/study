package spring.study.member.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.service.MemberService;

@Service
@RequiredArgsConstructor
public class RegistrationEmailFacade {
    private static final String ATTEMPT_ATTRIBUTE = "registrationEmailAttempt";
    private static final String VERIFY_REDIRECT = "redirect:/member/register/verify";
    private static final String REGISTER_REDIRECT = "redirect:/member/register";

    private final EmailVerificationService emailVerificationService;
    private final MemberService memberService;

    public String registration(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String email = emailVerificationService.verifiedRegistrationEmail(currentAttempt(request));
        if (email == null) return VERIFY_REDIRECT;

        model.addAttribute("email", email);
        return "member/register";
    }

    public String verification(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String attempt = currentAttempt(request);
        if (emailVerificationService.verifiedRegistrationEmail(attempt) != null) return REGISTER_REDIRECT;

        model.addAttribute("pendingEmail", emailVerificationService.pendingRegistrationEmail(attempt));
        return "member/register-email";
    }

    public String send(String email, Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (memberService.existEmail(email.trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다");
            }
            String previous = currentAttempt(request);
            String attempt = emailVerificationService.sendRegistrationCode(email);
            request.getSession().setAttribute(ATTEMPT_ATTRIBUTE, attempt);
            emailVerificationService.clearRegistration(previous);
            return VERIFY_REDIRECT;
        } catch (ResponseStatusException exception) {
            return verificationError(exception, model, request, response);
        } catch (IllegalArgumentException exception) {
            return verificationError(new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 확인해 주세요"),
                    model, request, response);
        } catch (IllegalStateException exception) {
            return verificationError(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 발송하지 못했습니다"),
                    model, request, response);
        }
    }

    public String verify(String code, Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            emailVerificationService.verifyRegistrationCode(currentAttempt(request), code);
            return REGISTER_REDIRECT;
        } catch (ResponseStatusException exception) {
            return verificationError(exception, model, request, response);
        }
    }

    public String restart(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            emailVerificationService.clearRegistration(currentAttempt(request));
            session.removeAttribute(ATTEMPT_ATTRIBUTE);
        }
        return VERIFY_REDIRECT;
    }

    public static String currentAttempt(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(ATTEMPT_ATTRIBUTE);
    }

    private String verificationError(ResponseStatusException exception, Model model,
                                     HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(exception.getStatusCode().value());
        model.addAttribute("verificationError", exception.getReason());
        return verification(model, request, response);
    }
}
