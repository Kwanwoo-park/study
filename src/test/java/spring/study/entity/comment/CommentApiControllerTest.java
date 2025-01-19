//package spring.study.entity.comment;
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
//import spring.study.dto.comment.CommentRequestDto;
//import spring.study.entity.member.Member;
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
//public class CommentApiControllerTest {
//    @LocalServerPort
//    private int port;
//
//    String url = "http://localhost:" + port + "/comment";
//
//    @Autowired
//    private TestRestTemplate testTemplate;
//    @Autowired
//    private MemberService memberService;
//    @Autowired
//    private WebApplicationContext context;
//    private MockMvc mvc;
//
//    @WithMockUser
//    @Test
//    void test() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        url += "/37/action";
//
//        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
//                .comments("test37")
//                .build();
//
//        // when
//        mvc.perform(post(url).session(session)
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(new ObjectMapper().writeValueAsString(commentRequestDto))
//        ).andExpect(status().isOk());
//
//        // then
//        Member member = memberService.findMember("test@test.com");
//
//        assertThat(member.getComment().get(3).getComments()).isEqualTo(commentRequestDto.getComments());
//    }
//}
