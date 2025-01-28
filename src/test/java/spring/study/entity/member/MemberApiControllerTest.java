package spring.study.entity.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.service.member.MemberService;

import java.io.FileInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class MemberApiControllerTest {
    @LocalServerPort
    private int port;

    String url = "http://localhost:" + port + "/member";

    @Autowired
    private TestRestTemplate testTemplate;
    @Autowired
    private MemberService memberService;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @WithMockUser(roles = "USER")
    @Test
    void login() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MockHttpSession session = new MockHttpSession();

        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("email", "test@test.com");
        data.add("password", "test");

        url += "/login/action";

        // when
        mvc.perform(get(url).session(session)
                        .params(data)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "USER")
    @Test
    void save() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
                .email("test2@test.com")
                .password("test")
                .name("test")
                .role(Role.USER)
                .phone("010-1234-1234")
                .birth("1900-01-01")
                .build();

         url += "/register/action";

        // when
        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(new ObjectMapper().writeValueAsString(memberRequestDto)))
                .andExpect(status().isOk())
                .andDo(print());

        // then
        Member member = memberService.findMember("test2@test.com");
        assertThat(member.getEmail()).isEqualTo("test2@test.com");
        assertThat(member.getProfile()).isEqualTo("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg");
        assertThat(member.getName()).isEqualTo("test");
    }

    @WithMockUser(roles = "USER")
    @Test
    void updateProfile() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("member", memberService.findMember("test2@test.com"));

        final String file = "/Users/lg/Desktop/study/study/src/main/resources/static/img/IMG_0111.jpeg";

        FileInputStream fileInputStream = new FileInputStream(file);

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "IMG_0111.jpeg",
                "jpeg",
                fileInputStream
        );

        url += "/detail/action";

        // when
        mvc.perform(
                MockMvcRequestBuilders.multipart(HttpMethod.PATCH, url).file(image)
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isOk());

        // then
        Member member = memberService.findMember("test2@test.com");
        assertThat(member.getProfile()).isEqualTo("IMG_0111.jpeg");

        session.invalidate();
    }

    @WithMockUser(roles = "USER")
    @Test
    void find() throws Exception{
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        url += "/find/email=test2@test.com/action";

        // when
        mvc.perform(get(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(request().sessionAttributeDoesNotExist("member"));
    }

    @WithMockUser
    @Test
    void updatePassword() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
                .email("test2@test.com")
                .password("test2")
                .build();

        url += "/updatePassword/action";

        // when
        mvc.perform(patch(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(memberRequestDto))
        ).andExpect(status().isOk());

        // then
        Member member = memberService.findMember("test2@test.com");

        if (new BCryptPasswordEncoder().matches("test2", member.getPassword()))
            System.out.println("Pass!!");
        else
            System.out.println("Fail!!");
    }

    @WithMockUser
    @Test
    void search() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MockHttpSession session = new MockHttpSession();

        url += "/search/name=test/action";

        // when
        mvc.perform(get(url).session(session)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(request().sessionAttributeDoesNotExist("member"));
    }

    @WithMockUser
    @Test
    void withdrawal() throws Exception {
        // given
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("member", memberService.findMember("test2@test.com"));

        url += "/withdrawal/action";

        // when
        mvc.perform(delete(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());

        // then
        Member member = memberService.findMember("test2@test.com");

        if (member == null)
            System.out.println("Pass!!");
        else
            System.out.println("Fail!!");
    }
}
