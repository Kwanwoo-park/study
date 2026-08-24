package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import spring.study.admin.dto.AdminNewBoardResponseDto;
import spring.study.admin.facade.AdminFacade;
import spring.study.admin.service.SystemIncidentService;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.common.service.OnlineUserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminNewBoardRegressionTest {

    @Mock private MemberService memberService;
    @Mock private BoardService boardService;
    @Mock private ChatMessageService chatMessageService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private SystemIncidentService systemIncidentService;
    @Mock private AudioCallSignalingService audioCallSignalingService;
    @Mock private OnlineUserService onlineUserService;

    @InjectMocks
    private AdminFacade adminFacade;

    @Test
    void administratorShouldBeAbleToForceTerminateAnAudioCall() {
        Map<?, ?> body = (Map<?, ?>) adminFacade.forceTerminateAudioCall("call-1").getBody();

        verify(audioCallSignalingService).forceTerminate("call-1");
        assertEquals(1L, body.get("result"));
    }

    @Test
    void newBoardShouldReturnTheBoardSummaryWithItsFirstImage() {
        Member member = Member.builder()
                .id(1L)
                .email("writer@test.com")
                .pwd("pwd")
                .name("작성자")
                .role(Role.USER)
                .phone("010-1111-1111")
                .birth("2000-01-01")
                .profile("profile")
                .build();
        Board board = Board.builder()
                .id(10L)
                .content("content")
                .member(member)
                .build();
        board.addImg(BoardImg.builder().id(100L).imgSrc("first-image").build());
        board.addImg(BoardImg.builder().id(101L).imgSrc("second-image").build());

        when(boardService.findNewBoard(any(), any())).thenReturn(List.of(board));

        Map<?, ?> body = (Map<?, ?>) adminFacade.newBoard().getBody();
        List<?> list = (List<?>) body.get("list");
        AdminNewBoardResponseDto response = (AdminNewBoardResponseDto) list.get(0);

        assertEquals(1, body.get("count"));
        assertEquals(10L, response.getId());
        assertEquals("작성자", response.getMemberName());
        assertEquals("first-image", response.getImageUrl());
    }

    @Test
    void adminJavascriptShouldUseTheNewBoardMemberNameField() throws Exception {
        String adminJs = Files.readString(Path.of("src/main/resources/static/js/admin/main.js"));
        String adminTemplate = Files.readString(Path.of("src/main/resources/templates/admin/administrator.html"));
        String adminCss = Files.readString(Path.of("src/main/resources/static/css/admin/admin.css"));

        assertTrue(adminJs.contains("board.memberName"));
        assertTrue(adminJs.contains("imageUrl: member.profile"));
        assertTrue(adminJs.contains("imageUrl: board.imageUrl"));
        assertFalse(adminJs.contains("board.member.name"));
        assertTrue(adminJs.contains("renderActivityCard(newBoardDiv"));
        assertTrue(adminJs.contains("row.addEventListener('keydown'"));
        assertTrue(adminTemplate.contains("class=\"admin-activity-grid\""));
        assertTrue(adminTemplate.contains("admin-activity-card-board"));
        assertTrue(adminCss.contains("grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(adminCss.contains(".admin-activity-item.is-clickable:focus-visible"));
        assertTrue(adminCss.contains(".admin-activity-item-image"));
        assertTrue(adminTemplate.contains("id=\"system-incident-list\""));
        assertTrue(adminTemplate.contains("id=\"incident-acknowledge-all\""));
        assertTrue(adminTemplate.contains("/js/admin/main.js?v=20260824"));
        assertTrue(adminJs.contains("/api/admin/system/incidents"));
        assertTrue(adminJs.contains("acknowledgeSystemIncident"));
        assertTrue(adminJs.contains("acknowledgeAllSystemIncidents"));
        assertTrue(adminJs.contains("/api/admin/system/incidents/acknowledge-all"));
        assertTrue(adminJs.contains("incident.requestIp"));
        assertTrue(adminCss.contains(".admin-incident-dashboard"));
        assertTrue(adminCss.contains("body.dark-mode .admin-incident-count"));
    }
}
