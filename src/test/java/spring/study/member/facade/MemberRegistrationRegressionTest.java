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
import spring.study.mail.service.EmailVerificationService;

import java.util.Map;

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
    void memberAndResponseNeverSerializePasswords() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        Member member = Member.builder().id(1L).role(Role.USER).pwd("private-hash").build();
        assertThat(mapper.valueToTree(member).has("password")).isFalse();
        assertThat(mapper.valueToTree(new MemberResponseDto(member)).has("password")).isFalse();
    }

    @Test
    void registrationWithoutEmailProofDoesNotCreateAMember() {
        MemberRequestDto request = MemberRequestDto.builder()
                .email("new@test.com").password("password").name("new member")
                .phone("010-1111-2222").birth("2000-01-01").build();

        assertThat(memberFacade.register(request).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(userService);
    }

    @Test
    void registrationReturnsPersistedMemberId() {
        MemberRequestDto request = MemberRequestDto.builder()
                .email("new@test.com").password("password").name("new member")
                .phone("010-1111-2222").birth("2000-01-01").build();
        Member saved = Member.builder().id(42L).email(request.getEmail()).pwd("encoded").name(request.getName())
                .role(Role.USER).phone(request.getPhone()).birth(request.getBirth()).profile("profile").build();
        Member admin = Member.builder().id(1L).email("admin@test.com").pwd("pwd").name("admin")
                .role(Role.ADMIN).phone("010-0000-0000").birth("2000-01-01").profile("profile").build();
        when(forbiddenService.findWordList(Status.APPROVAL, request.getName())).thenReturn(0);
        when(userService.createUser(request)).thenReturn(new MemberResponseDto(saved));
        when(memberService.findAdministrator()).thenReturn(admin);
        request.setVerificationToken("verified-token");
        when(emailVerificationService.consume(request.getEmail(), "verified-token")).thenReturn(true);

        var response = memberFacade.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("result")).isEqualTo(42L);
    }
}
