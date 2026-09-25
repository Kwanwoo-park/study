package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import spring.study.admin.facade.AdminFacade;
import spring.study.admin.service.SystemDiagnosticsService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.report.facade.ReportFacade;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSystemDiagnosticsControllerTest {
    @Mock private JwtManager jwtManager;
    @Mock private CommonFacade commonFacade;
    @Mock private AdminFacade adminFacade;
    @Mock private SystemDiagnosticsService systemDiagnosticsService;
    @Mock private MemberFacade memberFacade;
    @Mock private ForbiddenFacade forbiddenFacade;
    @Mock private ReportFacade reportFacade;
    @InjectMocks private AdminApiController controller;

    @Test
    void administratorCanInspectProcessesAndDisk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtManager.getLoginMember(request)).thenReturn(Member.builder().role(Role.ADMIN).build());
        var processes = new SystemDiagnosticsService.ProcessSnapshot(0, java.util.List.of());
        var disk = new SystemDiagnosticsService.DiskSnapshot(java.util.List.of(), "project", "", 0, 100, 0, java.util.List.of());
        when(systemDiagnosticsService.processes()).thenReturn(processes);
        when(systemDiagnosticsService.disk("project", "", 0)).thenReturn(disk);
        assertSame(processes, controller.systemProcesses(request).getBody());
        assertSame(disk, controller.systemDisk("project", "", 0, request).getBody());
    }

    @Test
    void regularMemberCannotInspectProcessesOrDisk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ResponseEntity<Object> denied = ResponseEntity.status(401).build();
        when(jwtManager.getLoginMember(request)).thenReturn(Member.builder().role(Role.USER).build());
        when(commonFacade.wrongAccess()).thenReturn(denied);
        assertSame(denied, controller.systemProcesses(request));
        assertSame(denied, controller.systemDisk("project", "", 0, request));
        verifyNoInteractions(systemDiagnosticsService);
    }

    @Test
    void anonymousVisitorCannotInspectProcessesOrDisk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ResponseEntity<Object> denied = ResponseEntity.status(401).build();
        when(commonFacade.unauthorized()).thenReturn(denied);
        assertSame(denied, controller.systemProcesses(request));
        assertSame(denied, controller.systemDisk("project", "", 0, request));
        verifyNoInteractions(systemDiagnosticsService);
    }
}
