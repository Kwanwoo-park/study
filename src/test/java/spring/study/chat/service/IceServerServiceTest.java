package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import spring.study.chat.dto.IceServerDto;
import spring.study.member.entity.Member;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IceServerServiceTest {
    @Test
    void shouldCreateTemporaryTurnCredentialForTheAuthenticatedMember() {
        IceServerService service = new IceServerService(
                "stun:stun.example.com:3478",
                List.of("turn:turn.example.com:3478?transport=udp", "turn:turn.example.com:3478?transport=tcp"),
                "test-shared-secret",
                3600);
        Member member = Member.builder().id(42L).email("member@test.com").build();

        List<IceServerDto> servers = service.createIceServers(member);

        assertEquals(2, servers.size());
        assertEquals("stun:stun.example.com:3478", servers.get(0).urls().get(0));
        assertTrue(servers.get(1).username().endsWith(":42"));
        assertFalse(servers.get(1).credential().isBlank());
        assertEquals(2, servers.get(1).urls().size());
    }

    @Test
    void shouldOmitTurnWhenItIsNotConfigured() {
        IceServerService service = new IceServerService(
                "stun:stun.example.com:3478", List.of(), "", 3600);

        List<IceServerDto> servers = service.createIceServers(
                Member.builder().id(1L).email("member@test.com").build());

        assertEquals(1, servers.size());
        assertEquals("stun:stun.example.com:3478", servers.get(0).urls().get(0));
    }
}
