package spring.study.entity.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.MemberService;

import java.io.FileInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class MemberApiControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testTemplate;
    @Autowired
    private MemberService memberService;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @WithMockUser(roles = "USER")
    @Test
    void save() throws Exception {
        // given
        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
                .email("test2@test.com")
                .password("test")
                .name("test")
                .role(Role.USER)
                .profile("1.img")
                .build();

        String url = "http://localhost:" + port + "/member/register/action";

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // when
        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(new ObjectMapper().writeValueAsString(memberRequestDto)))
                .andExpect(status().isOk());

        // then
        Member member = memberService.findMember("test@test.com");
        assertThat(member.getEmail()).isEqualTo("test@test.com");
        assertThat(member.getProfile()).isEqualTo("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg");
        assertThat(member.getName()).isEqualTo("test");
    }

    @WithMockUser(roles = "USER")
    @Test
    void update() throws Exception {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("member", memberService.findMember("test@test.com"));

        final String file = "/Users/lg/Desktop/study/study/src/main/resources/static/img/IMG_0111.jpeg";

        FileInputStream fileInputStream = new FileInputStream(file);

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "IMG_0111.jpeg",
                "jpeg",
                fileInputStream
        );

        String url = "http://localhost:" + port + "/member/detail/action";

        mvc.perform(
                MockMvcRequestBuilders.multipart(HttpMethod.PATCH, url).file(image)
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isOk());

        Member member = memberService.findMember("test@test.com");
        assertThat(member.getProfile()).isEqualTo("IMG_0111.jpeg");

        session.invalidate();
    }
}
