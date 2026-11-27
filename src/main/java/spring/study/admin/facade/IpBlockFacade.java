package spring.study.admin.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.service.IpBlockService;
import spring.study.common.component.IpBlockClientAddress;
import spring.study.common.service.ClientIpResolver;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

@Service
@RequiredArgsConstructor
public class IpBlockFacade {
    private final IpBlockService blocks;
    private final IpBlockClientAddress clientAddress;
    private final EmailVerificationService emailVerificationService;

    public String list(Model model, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        model.addAttribute("blocks", blocks.list());
        model.addAttribute("emailBlocks", emailVerificationService.listBlockedEmails());
        return "admin/ip-blocks";
    }

    public String block(String input, Member admin, HttpServletRequest request,
                        HttpServletResponse response, Model model) {
        requireAdmin(admin, "관리자만 IP를 차단할 수 있습니다.");

        String address = ClientIpResolver.canonicalAddress(input);
        if (address == null) {
            return invalid("error", "IPv4 또는 IPv6 주소 한 개를 입력해 주세요.", response, model);
        }
        if (address.equals(clientAddress.resolve(request))
                || address.equals(ClientIpResolver.canonicalAddress(request.getRemoteAddr()))) {
            return invalid("error", "현재 접속에 사용 중인 IP는 차단할 수 없습니다.", response, model);
        }

        try {
            blocks.block(address, admin.getId());
        } catch (DataIntegrityViolationException concurrentInsert) {
            if (!blocks.isBlocked(address)) throw concurrentInsert;
        }
        return "redirect:/admin/ip-blocks";
    }

    public String blockEmail(String email, Member admin, HttpServletResponse response, Model model) {
        requireAdmin(admin, "관리자만 이메일을 차단할 수 있습니다.");

        try {
            emailVerificationService.blockEmail(email);
        } catch (IllegalArgumentException invalidEmail) {
            return invalid("emailError", invalidEmail.getMessage(), response, model);
        }
        return "redirect:/admin/ip-blocks#blocked-email-title";
    }

    private void requireAdmin(Member admin, String message) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String invalid(String attribute, String message, HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute(attribute, message);
        return list(model, response);
    }
}
