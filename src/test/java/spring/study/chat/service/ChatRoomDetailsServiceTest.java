package spring.study.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.repository.ChatMessageImgRepository;
import spring.study.chat.repository.ChatRoomMemberRepository;
import spring.study.chat.repository.ChatRoomRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRoomDetailsServiceTest {
    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatRoomMemberRepository participants = mock(ChatRoomMemberRepository.class);
    private final ChatMessageImgRepository images = mock(ChatMessageImgRepository.class);
    private final ChatRoomDetailsService service = new ChatRoomDetailsService(rooms, participants, images);
    private final Member viewer = Member.builder().id(1L).email("viewer@example.test").name("나").role(Role.USER).build();
    private final ChatRoom room = ChatRoom.builder().id(1L).roomId("room-a").name("우리 방").count(10L).build();

    @BeforeEach
    void setUp() {
        when(rooms.findByRoomId("room-a")).thenReturn(room);
        when(participants.existsByMemberAndRoom(viewer, room)).thenReturn(true);
    }

    @Test
    void unauthenticatedAndOutsidersIncludingAdminsCannotInspectTheRoom() {
        assertStatus(() -> service.details("room-a", null), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> service.images("room-a", null, null), HttpStatus.UNAUTHORIZED);
        for (Role role : List.of(Role.USER, Role.ADMIN)) {
            Member outsider = Member.builder().id(2L).role(role).build();
            assertStatus(() -> service.details("room-a", outsider), HttpStatus.FORBIDDEN);
            assertStatus(() -> service.images("room-a", outsider, 100L), HttpStatus.FORBIDDEN);
        }
        verifyNoInteractions(images);
        verify(participants, never()).findParticipantsByRoom(any());
    }

    @Test
    void missingRoomsReturnNotFound() {
        assertStatus(() -> service.details("missing", viewer), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.images("missing", viewer, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void rechecksMembershipOnEveryGalleryPageAfterLeaving() {
        when(images.findVisibleRoomImages(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        assertThat(service.images("room-a", viewer, null).images()).isEmpty();
        when(participants.existsByMemberAndRoom(viewer, room)).thenReturn(false);
        assertStatus(() -> service.images("room-a", viewer, 1L), HttpStatus.FORBIDDEN);
        verify(images, times(1)).findVisibleRoomImages(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void showsRealParticipantCountAndOwnMemberMarkerWithoutMemberEntityPayload() {
        when(participants.findParticipantsByRoom(room)).thenReturn(List.of(ChatRoomMember.builder().room(room).member(viewer).build()));
        var response = service.details("room-a", viewer);
        assertThat(response.name()).isEqualTo("우리 방");
        assertThat(response.participants()).hasSize(1);
        assertThat(response.participants().get(0).me()).isTrue();
    }

    @Test
    void fetchesOneExtraImageAndReturnsTheLastVisibleIdAsCursor() {
        var found = LongStream.rangeClosed(1, 25).mapToObj(id -> new ChatRoomImageResponse(26 - id, "message", "url", LocalDateTime.now(), "sender")).toList();
        when(images.findVisibleRoomImages(eq(room), eq(viewer), isNull(), any(), any(), any(), any())).thenReturn(found);
        var response = service.images("room-a", viewer, null);
        assertThat(response.images()).hasSize(24);
        assertThat(response.nextCursor()).isEqualTo(2L);
        verify(images).findVisibleRoomImages(eq(room), eq(viewer), isNull(), any(), any(), any(), argThat(page -> page.getPageSize() == 25));
    }

    @Test
    void invalidCursorIsRejectedWithoutImageQuery() {
        assertStatus(() -> service.images("room-a", viewer, 0L), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.images("room-a", viewer, -1L), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(images);
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(status));
    }
}
