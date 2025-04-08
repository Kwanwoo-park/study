//package spring.study.entity.board;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import spring.study.dto.board.BoardRequestDto;
//import spring.study.service.board.BoardService;
//import spring.study.service.member.MemberService;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class BoardApiControllerTest {
//    @LocalServerPort
//    private int port;
//
//    String url = "http://localhost:" + port + "/api/board";
//
//    @Autowired
//    private TestRestTemplate testTemplate;
//    @Autowired
//    private MemberService memberService;
//    @Autowired
//    private BoardService boardService;
//    @Autowired
//    private WebApplicationContext context;
//    private MockMvc mvc;
//
//    @WithMockUser
//    @Test
//    void write() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        BoardRequestDto boardRequestDto = BoardRequestDto.builder()
//                .content("test")
//                .build();
//
//        url += "/write/action";
//
//        // when
//        mvc.perform(post(url).session(session)
//                .content(new ObjectMapper().writeValueAsString(boardRequestDto))
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().isOk());
//
//        // then
//        Board board = boardService.findById(39L);
//
//        assertThat(board.getContent()).isEqualTo("test");
//        assertThat(board.getMember().getEmail()).isEqualTo("test@test.com");
//
//        session.invalidate();
//    }
//
//    @WithMockUser
//    @Test
//    void viewUpdate() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        BoardRequestDto boardRequestDto = BoardRequestDto.builder()
//                .id(39L)
//                .content("test2")
//                .build();
//
//        url += "/view/action";
//
//        // when
//        mvc.perform(patch(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(new ObjectMapper().writeValueAsString(boardRequestDto))
//        ).andExpect(status().isOk());
//
//        // then
//        Board board = boardService.findById(39L);
//
//        assertThat(board.getContent()).isEqualTo(boardRequestDto.getContent());
//    }
//
//    @WithMockUser
//    @Test
//    void deleteBoard() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        url += "/view/delete?id=39";
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        // when
//        mvc.perform(delete(url).session(session)
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().isOk());
//    }
//}
