package spring.study.appeal.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.appeal.repository.AppealRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.report.entity.Report;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {
    @Mock
    private AppealRepository appealRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberSanctionRepository memberSanctionRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppealService appealService;

    @Test
    void authenticatedMemberCanCreateAppealWithoutPasswordAndNotifyEveryAdministrator() {
        Member member = member(1L, "member@example.com", Role.USER);
        Member firstAdmin = member(2L, "admin1@example.com", Role.ADMIN);
        Member secondAdmin = member(3L, "admin2@example.com", Role.ADMIN);
        AppealRequestDto request = request("  처리 재검토 요청  ", "  사실관계를 다시 확인해주세요.  ");

        when(appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 20L));
        when(memberRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(firstAdmin, secondAdmin));

        AppealResponseDto response = appealService.create(request, member);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTitle()).isEqualTo("처리 재검토 요청");
        assertThat(response.getContent()).isEqualTo("사실관계를 다시 확인해주세요.");
        verify(memberRepository, never()).findByEmail(any());
        verify(notificationService).createNotification(
                eq(firstAdmin), eq("member님이 상소문을 제출했습니다."), eq(Group.ADMIN),
                eq("/admin/appeal?appealId=20"));
        verify(notificationService).createNotification(
                eq(secondAdmin), eq("member님이 상소문을 제출했습니다."), eq(Group.ADMIN),
                eq("/admin/appeal?appealId=20"));
    }

    @Test
    void anonymousMemberCanCreateAppealAfterPasswordVerification() {
        Member member = member(1L, "member@example.com", Role.USER);
        AppealRequestDto request = request("재검토", "상소 내용");
        request.setEmail(" member@example.com ");
        request.setPassword("plain-password");

        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("plain-password", member.getPassword())).thenReturn(true);
        when(appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 21L));
        when(memberRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        AppealResponseDto response = appealService.create(request, null);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getMemberEmail()).isEqualTo("member@example.com");
        verify(passwordEncoder).matches("plain-password", member.getPassword());
    }

    @Test
    void anonymousMemberWithWrongPasswordCannotCreateAppeal() {
        Member member = member(1L, "member@example.com", Role.USER);
        AppealRequestDto request = request("재검토", "상소 내용");
        request.setEmail("member@example.com");
        request.setPassword("wrong-password");

        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> appealService.create(request, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(notificationService);
        verify(appealRepository, never()).save(any());
    }

    @Test
    void memberCannotAttachAnotherMembersSanction() {
        Member member = member(1L, "member@example.com", Role.USER);
        AppealRequestDto request = request("재검토", "상소 내용");
        request.setSanctionId(99L);

        when(appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)).thenReturn(false);
        when(memberSanctionRepository.findByIdAndMember(99L, member)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.create(request, member))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(appealRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void memberCannotCreateAnotherAppealWhileOneIsPending() {
        Member member = member(1L, "member@example.com", Role.USER);
        AppealRequestDto request = request("재검토", "상소 내용");
        when(appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> appealService.create(request, member))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(appealRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void selectedSanctionMustBelongToAuthenticatedMember() {
        Member member = member(1L, "member@example.com", Role.USER);
        MemberSanction sanction = org.mockito.Mockito.mock(MemberSanction.class);
        Report report = org.mockito.Mockito.mock(Report.class);
        AppealRequestDto request = request("재검토", "상소 내용");
        request.setSanctionId(30L);

        when(appealRepository.existsByMemberAndStatus(member, AppealStatus.PENDING)).thenReturn(false);
        when(memberSanctionRepository.findByIdAndMember(30L, member)).thenReturn(Optional.of(sanction));
        when(sanction.getId()).thenReturn(30L);
        when(sanction.getReport()).thenReturn(report);
        when(report.getId()).thenReturn(40L);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 22L));
        when(memberRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        appealService.create(request, member);

        ArgumentCaptor<Appeal> captor = ArgumentCaptor.forClass(Appeal.class);
        verify(appealRepository).save(captor.capture());
        assertThat(captor.getValue().getRelatedSanction()).isSameAs(sanction);
    }

    private AppealRequestDto request(String title, String content) {
        AppealRequestDto request = new AppealRequestDto();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private Appeal persisted(Appeal source, Long id) {
        return Appeal.builder()
                .id(id)
                .member(source.getMember())
                .relatedSanction(source.getRelatedSanction())
                .title(source.getTitle())
                .content(source.getContent())
                .status(source.getStatus())
                .build();
    }

    private Member member(Long id, String email, Role role) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("encoded-password")
                .name("member")
                .role(role)
                .phone("0100000000" + id)
                .birth("20000101")
                .profile("profile.png")
                .build();
    }
}
