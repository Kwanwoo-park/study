package spring.study.member.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.service.NotificationService;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberRegistrationRegressionTest {
    @Mock MemberService memberService;
    @Mock UserService userService;
    @Mock ForbiddenService forbiddenService;
    @Mock NotificationService notificationService;
    @Mock EmailVerificationService emailVerificationService;
    @InjectMocks MemberFacade memberFacade;

    @Test
    void registrationWithoutEmailProofDoesNotCreateAMember() {
        MemberRequestDto request = MemberRequestDto.builder()
                .email("new@test.com").password("password").name("new member")
                .phone("010-1111-2222").birth("2000-01-01").build();

        request.setVerificationToken("password-recovery-proof");
        assertThat(memberFacade.register(request, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(userService);
    }

    @Test
    void blankRequiredNameIsRejectedBeforeVerificationOrMemberCreation() {
        MemberRequestDto request = MemberRequestDto.builder()
                .email("new@test.com").password("password").name("   ")
                .phone("010-1111-2222").birth("2000-01-01").build();

        ResponseEntity<?> result = memberFacade.register(request, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isEqualTo(Map.of("result", -10L, "message", "이름이 입력되지 않았습니다"));
        verifyNoInteractions(userService, forbiddenService, emailVerificationService, notificationService);
    }

}
