package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;
import spring.study.member.service.PasswordChangeVerificationService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberPasswordChangeControllerTest {
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final CommonFacade commonFacade = mock(CommonFacade.class);
    private final MemberFacade memberFacade = mock(MemberFacade.class);
    private final MemberService memberService = mock(MemberService.class);
    private final JwtAuthenticationService authenticationService = mock(JwtAuthenticationService.class);
    private final PasswordChangeVerificationService verificationService = mock(PasswordChangeVerificationService.class);
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse httpResponse = mock(HttpServletResponse.class);
    private MemberApiController controller;
    private Member member;

    @BeforeEach
    void setUp() {
        controller = new MemberApiController(
                jwtManager,
                commonFacade,
                memberFacade,
                memberService,
                authenticationService,
                verificationService
        );
        member = Member.builder().id(7L).email("member@example.com").build();
    }

    @Test
    void authenticatedPasswordChangeShouldRequireUnusedEmailVerification() {
        MemberRequestDto request = MemberRequestDto.builder().password("new-password").build();
        when(jwtManager.getLoginMember(httpRequest)).thenReturn(member);
        when(verificationService.consumeVerification(member)).thenReturn(false);

        ResponseEntity<?> response = controller.updateAuthenticatedPassword(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(memberFacade, never()).updatePassword("new-password", member);
        verify(authenticationService, never()).logout(httpRequest, httpResponse);
    }

    @Test
    void authenticatedPasswordChangeShouldConsumeVerificationAndLogoutAfterSuccess() {
        MemberRequestDto request = MemberRequestDto.builder().password("new-password").build();
        when(jwtManager.getLoginMember(httpRequest)).thenReturn(member);
        when(verificationService.consumeVerification(member)).thenReturn(true);
        doReturn(ResponseEntity.ok(Map.of("result", 7L)))
                .when(memberFacade).updatePassword("new-password", member);

        ResponseEntity<?> response = controller.updateAuthenticatedPassword(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(verificationService).consumeVerification(member);
        verify(memberFacade).updatePassword("new-password", member);
        verify(authenticationService).logout(httpRequest, httpResponse);
    }

    @Test
    void recoveryPasswordChangeShouldRemainOnExistingUnauthenticatedFlow() {
        MemberRequestDto request = MemberRequestDto.builder()
                .email("member@example.com")
                .password("new-password")
                .build();
        when(jwtManager.getLoginMember(httpRequest)).thenReturn(null);
        when(memberService.findMember("member@example.com")).thenReturn(member);
        doReturn(ResponseEntity.ok(Map.of("result", 7L)))
                .when(memberFacade).updatePassword("new-password", member);

        ResponseEntity<?> response = controller.updatePasswordAction(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(memberFacade).updatePassword("new-password", member);
        verify(verificationService, never()).consumeVerification(member);
    }
}
