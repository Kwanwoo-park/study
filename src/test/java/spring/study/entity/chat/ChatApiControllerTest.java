//package spring.study.entity.chat;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import spring.study.dto.chat.ChatRoomRequestDto;
//import spring.study.dto.member.MemberRequestDto;
//import spring.study.entity.chat.ChatRoomMember;
//import spring.study.entity.member.Member;
//import spring.study.service.chat.ChatRoomMemberService;
//import spring.study.service.chat.ChatRoomService;
//import spring.study.service.member.MemberService;
//
//import java.io.FileInputStream;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class ChatApiControllerTest {
//    @LocalServerPort
//    private int port;
//
//    String url = "http://localhost:" + port + "/api/chat";
//
//    @Autowired
//    private TestRestTemplate testTemplate;
//    @Autowired
//    private MemberService memberService;
//    @Autowired
//    private ChatRoomService roomService;
//    @Autowired
//    private ChatRoomMemberService roomMemberService;
//    @Autowired
//    private WebApplicationContext context;
//    private MockMvc mvc;
//
//    @WithMockUser
//    @Test
//    void 채팅방생성() throws Exception {
//        //given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        Member member = memberService.findMember("test@test.com");
//        session.setAttribute("member", member);
//
//        url += "/createRoom";
//
//        ChatRoomRequestDto chatRoomRequestDto = ChatRoomRequestDto.builder()
//                .name("방생성 테스트")
//                .build();
//
//        //when
//        mvc.perform(post(url).session(session)
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(new ObjectMapper().writeValueAsString(chatRoomRequestDto))
//        ).andExpect(status().isOk());
//
//        //then
//        List<ChatRoomMember> list = roomMemberService.find(member);
//        String roomId = list.get(list.size()-1).getRoom().getRoomId();
//
//        assertThat(roomService.find(roomId), is(notNullValue()));
//    }
//
//    @WithMockUser
//    @Test
//    void create() throws Exception {
//        //given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        Member member = memberService.findMember("test@test.com");
//        session.setAttribute("member", member);
//
//        url += "/create";
//
//        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
//                .email("akakslslzz@naver.com")
//                .build();
//
//        //when
//        mvc.perform(post(url).session(session)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(new ObjectMapper().writeValueAsString(memberRequestDto))
//        ).andExpect(status().isOk()).andDo(print());
//    }
//
//    @WithMockUser
//    @Test
//    void sendImage() throws Exception {
//        //given
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .apply(springSecurity())
//                .build();
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("member", memberService.findMember("test@test.com"));
//
//        final String file = "/Users/lg/Desktop/study/study/src/main/resources/static/img/IMG_0111.jpeg";
//
//        FileInputStream fileInputStream = new FileInputStream(file);
//
//        MockMultipartFile image = new MockMultipartFile(
//                "file",
//                "IMG_0111.jpeg",
//                "jpeg",
//                fileInputStream
//        );
//
//        url += "/sendImage";
//
//        // when
//        mvc.perform(
//                MockMvcRequestBuilders.multipart(HttpMethod.POST, url).file(image)
//                        .session(session)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.MULTIPART_FORM_DATA)
//        ).andExpect(status().isOk()).andDo(print());
//    }
//}
