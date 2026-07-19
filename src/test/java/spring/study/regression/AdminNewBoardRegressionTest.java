package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import spring.study.admin.dto.AdminNewBoardResponseDto;
import spring.study.admin.facade.AdminFacade;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNewBoardRegressionTest {

    @Mock private MemberService memberService;
    @Mock private BoardService boardService;
    @Mock private ChatMessageService chatMessageService;
    @Mock private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AdminFacade adminFacade;

    @Test
    void newBoardShouldReturnOnlyTheBoardIdAndMemberName() {
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

        when(boardService.findNewBoard(any(), any())).thenReturn(List.of(board));

        Map<?, ?> body = (Map<?, ?>) adminFacade.newBoard().getBody();
        List<?> list = (List<?>) body.get("list");
        AdminNewBoardResponseDto response = (AdminNewBoardResponseDto) list.get(0);

        assertEquals(1, body.get("count"));
        assertEquals(10L, response.getId());
        assertEquals("작성자", response.getMemberName());
    }

    @Test
    void adminJavascriptShouldUseTheNewBoardMemberNameField() throws Exception {
        String adminJs = Files.readString(Path.of("src/main/resources/static/js/admin/main.js"));
        String adminTemplate = Files.readString(Path.of("src/main/resources/templates/admin/administrator.html"));
        String adminCss = Files.readString(Path.of("src/main/resources/static/css/admin/admin.css"));

        assertTrue(adminJs.contains("board.memberName"));
        assertFalse(adminJs.contains("board.member.name"));
        assertTrue(adminJs.contains("renderActivityCard(newBoardDiv"));
        assertTrue(adminJs.contains("row.addEventListener('keydown'"));
        assertTrue(adminTemplate.contains("class=\"admin-activity-grid\""));
        assertTrue(adminTemplate.contains("admin-activity-card-board"));
        assertTrue(adminCss.contains("grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(adminCss.contains(".admin-activity-item.is-clickable:focus-visible"));
    }
}
