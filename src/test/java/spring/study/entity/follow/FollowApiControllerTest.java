//package spring.study.entity.follow;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
//import spring.study.follow.dto.FollowRequestDto;
//import spring.study.dto.member.MemberRequestDto;
//import spring.study.service.member.MemberService;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class FollowApiControllerTest {
//    @LocalServerPort
//    private int port;
//
//    String url = "http://localhost:" + port + "/api/follow";
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
//    void follow() throws Exception{
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
//                .email("akakslslzz@naver.com")
//                .build();
//
//        url += "/action";
//
//        // when
//        mvc.perform(post(url).session(session)
//                .content(new ObjectMapper().writeValueAsString(memberRequestDto))
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().isOk());
//
//        // then
//        List<Follow> follower = memberService.findMember("test@test.com").getFollower();
//
//        assertThat(follower.get(0).getFollowing().getEmail()).isEqualTo(memberRequestDto.getEmail());
//    }
//
//    @WithMockUser
//    @Test
//    void duplicateCheck() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        FollowRequestDto followRequestDto = FollowRequestDto.builder()
//                .following(memberService.findMember("akakslslzz@naver.com"))
//                .build();
//
//        // when
//        mvc.perform(post(url).session(session)
//                .content(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(followRequestDto))
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().is5xxServerError());
//    }
//
//    @WithMockUser
//    @Test
//    void unfollow() throws Exception{
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
//                .email("akakslslzz@naver.com")
//                .build();
//
//        url += "/action";
//
//        // when
//        mvc.perform(delete(url).session(session)
//                .content(new ObjectMapper().writeValueAsString(memberRequestDto))
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().isOk());
//
//        // then
//        List<Follow> follower = memberService.findMember("test@test.com").getFollower();
//
//        if (follower.isEmpty())
//            System.out.println("Pass!!");
//        else
//            System.out.println("Fail!!");
//    }
//
//    @WithMockUser
//    @Test
//    void deleteDuplicateCheck() throws Exception {
//        // given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        FollowRequestDto requestDto = FollowRequestDto.builder()
//                .following(memberService.findMember("akakslslzz@naver.com"))
//                .build();
//
//        // when
//        mvc.perform(delete(url).session(session)
//                .content(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(requestDto))
//                .contentType(MediaType.APPLICATION_JSON)
//        ).andExpect(status().is5xxServerError());
//    }
//}
