package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import spring.study.admin.facade.AdminFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;
import spring.study.report.facade.ReportFacade;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAudioCallControllerTest {
    @Mock private MemberService memberService;
    @Mock private JwtManager jwtManager;
    @Mock private CommonFacade commonFacade;
    @Mock private AdminFacade adminFacade;
    @Mock private MemberFacade memberFacade;
    @Mock private ForbiddenFacade forbiddenFacade;
    @Mock private ReportFacade reportFacade;

    @InjectMocks
    private AdminApiController controller;

    @Test
    void administratorShouldBeAllowedToForceTerminateAnAudioCall() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Member administrator = Member.builder().id(1L).role(Role.ADMIN).build();
        ResponseEntity<?> expected = ResponseEntity.ok().build();
        when(jwtManager.getLoginMember(request)).thenReturn(administrator);
        doReturn(expected).when(adminFacade).forceTerminateAudioCall("call-1");

        ResponseEntity<?> response = controller.forceTerminateAudioCall("call-1", request);

        assertSame(expected, response);
        verify(adminFacade).forceTerminateAudioCall("call-1");
    }

    @Test
    void regularMemberShouldNotBeAllowedToForceTerminateAnAudioCall() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Member member = Member.builder().id(2L).role(Role.USER).build();
        ResponseEntity<?> expected = ResponseEntity.status(403).build();
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        doReturn(expected).when(commonFacade).wrongAccess();

        ResponseEntity<?> response = controller.forceTerminateAudioCall("call-1", request);

        assertSame(expected, response);
        verify(jwtManager).logout(request);
        verify(adminFacade, never()).forceTerminateAudioCall("call-1");
    }
}
