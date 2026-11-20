package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.jwt.service.JwtCookieService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberPasswordChangeControllerTest {
    @Mock private JwtManager jwtManager;
    @Mock private CommonFacade commonFacade;
    @Spy @InjectMocks private MemberFacade memberFacade;
    @Mock private MemberService memberService;
    @Mock private JwtCookieService cookieService;
    @Mock private EmailVerificationService emailVerificationService;
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse httpResponse = mock(HttpServletResponse.class);
    private MemberApiController controller;
    private Member member;

    @BeforeEach
    void setUp() {
        controller = new MemberApiController(
                jwtManager,
                commonFacade,
                memberFacade
        );
        member = Member.builder().id(7L).email("member@example.com").build();
    }

    @Test
    void authenticatedPasswordChangeShouldRequireUnusedEmailVerification() {
        MemberRequestDto request = MemberRequestDto.builder().password("new-password").build();
        when(jwtManager.getLoginMember(httpRequest)).thenReturn(member);
        when(emailVerificationService.consumePasswordChange("7")).thenReturn(false);

        ResponseEntity<?> response = controller.updateAuthenticatedPassword(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(memberFacade, never()).updatePassword("new-password", member);
        verify(cookieService, never()).clearAuthenticationCookies(httpResponse);
    }

    @Test
    void unauthenticatedRecoveryDoesNotLookUpTheAccountBeforeProof() {
        MemberRequestDto request = MemberRequestDto.builder().email("member@example.com").password("new-password").build();
        request.setVerificationToken("invalid-proof");
        ResponseEntity<?> response = controller.updatePasswordAction(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(emailVerificationService).consume(EmailVerificationPurpose.EMAIL, "member@example.com", "invalid-proof");
        verifyNoInteractions(memberService);
    }

}
