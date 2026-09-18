package spring.study.regression;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.aws.service.ImageS3Service;
import spring.study.aws.service.ImageCleanupService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberFacadeRegressionTest {
    @Mock private MemberService memberService;
    @Mock private ImageS3Service imageS3Service;
    @Mock private ImageCleanupService imageCleanupService;
    @Mock private BCryptPasswordEncoder encoder;
    @Mock private JwtAuthenticationService jwtAuthenticationService;

    @InjectMocks
    private MemberFacade memberFacade;

    @Test
    void successfulLoginPassesTheBrowserRequestForTokenReplacement() {
        Member member = Member.builder().id(1L).email("login@example.test").pwd("encoded").role(Role.USER).build();
        MemberRequestDto dto = MemberRequestDto.builder().email(member.getEmail()).password("password").build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(memberService.loadUserByUsername(member.getEmail())).thenReturn(member);
        when(encoder.matches("password", "encoded")).thenReturn(true);
        when(memberService.updateLastLoginTime(member.getId())).thenReturn(member);

        assertEquals(HttpStatus.OK, memberFacade.login(dto, request, response, "203.0.113.10").getStatusCode());

        verify(jwtAuthenticationService).login(member, request, response, "203.0.113.10");
    }

    @Test
    void failedLoginCannotReplaceAnExistingBrowserToken() {
        Member member = Member.builder().id(1L).email("login@example.test").pwd("encoded").role(Role.USER).build();
        MemberRequestDto dto = MemberRequestDto.builder().email(member.getEmail()).password("wrong").build();
        when(memberService.loadUserByUsername(member.getEmail())).thenReturn(member);

        assertEquals(HttpStatus.BAD_REQUEST, memberFacade.login(dto, new MockHttpServletRequest(),
                new MockHttpServletResponse(), "203.0.113.10").getStatusCode());

        verifyNoInteractions(jwtAuthenticationService);
        verify(memberService, never()).updateLastLoginTime(anyLong());
    }

    @Test
    void changeProfileImageShouldDeleteOldImageOnlyAfterSuccessfulUpload() throws Exception {
        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "img".getBytes());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(imageS3Service.uploadImageToS3(file)).thenReturn("https://cdn/new.png");

        var response = memberFacade.changeProfileImage(file, member, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        InOrder inOrder = inOrder(imageS3Service, memberService, imageCleanupService);
        inOrder.verify(imageS3Service).uploadImageToS3(file);
        inOrder.verify(memberService).updateProfile(1L, "https://cdn/new.png");
        inOrder.verify(imageCleanupService).enqueue("https://cdn/old.png");

        verifyNoInteractions(request);
    }

    @Test
    void changeProfileImageShouldNotDeleteOldImageWhenUploadFails() throws Exception {
        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "img".getBytes());
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(imageS3Service.uploadImageToS3(file)).thenThrow(new IOException("upload failed"));

        var response = memberFacade.changeProfileImage(file, member, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(imageS3Service, never()).deleteImage(anyString());
        verify(memberService, never()).updateProfile(anyLong(), anyString());
    }

    @Test
    void duplicateCheckShouldReturnConflictWhenEmailExists() {
        when(memberService.existEmail("dup@test.com")).thenReturn(true);

        var response = memberFacade.duplicateCheck("dup@test.com");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void updatePhoneShouldReturnConflictOnDuplicatePhoneAndBeSessionSafe() {
        MemberRequestDto dto = MemberRequestDto.builder()
                .email("a@test.com")
                .phone("010-9999-8888")
                .birth("2000-01-01")
                .build();

        Member member = Member.builder()
                .id(1L)
                .email("a@test.com")
                .pwd("pwd")
                .name("name")
                .role(Role.USER)
                .phone("010-1111-2222")
                .birth("2000-01-01")
                .profile("https://cdn/old.png")
                .build();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(memberService.findMember("a@test.com")).thenReturn(member);
        when(memberService.updatePhoneAndBirth(1L, "010-9999-8888", "2000-01-01")).thenReturn(-2);

        var response = memberFacade.updatePhone(dto, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
