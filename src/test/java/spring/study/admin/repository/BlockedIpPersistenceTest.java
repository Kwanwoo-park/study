package spring.study.admin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.service.IpBlockService;
import spring.study.common.component.IpBlockClientAddress;
import spring.study.common.component.IpBlockFilter;
import spring.study.common.service.ClientIpResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@Import(IpBlockService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BlockedIpPersistenceTest {
    @Autowired BlockedIpRepository repository;
    @Autowired IpBlockService service;

    @Test
    void committedBlockAppliesToNewFiltersAndDuplicateRegistrationKeepsOriginalRecord() throws Exception {
        String address = ClientIpResolver.canonicalAddress("2001:db8::4");
        service.block(address, 7L);
        var original = repository.findById(address).orElseThrow();
        service.block(address, 8L);
        var saved = repository.findById(address).orElseThrow();

        assertThat(saved.getBlockedAt()).isEqualTo(original.getBlockedAt());
        assertThat(saved.getBlockedByMemberId()).isEqualTo(7L);
        assertThat(service.list()).hasSize(1);

        var filter = new IpBlockFilter(new IpBlockClientAddress(List.of()), new IpBlockService(repository), new ObjectMapper());
        var request = new MockHttpServletRequest("POST", "/api/member/login");
        request.setRemoteAddr("2001:0db8:0:0:0:0:0:0004");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(chain.getRequest()).isNull();

        var allowed = new MockHttpServletRequest("POST", "/api/member/login");
        allowed.setRemoteAddr("2001:db8::5");
        filter.doFilter(allowed, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isSameAs(allowed);
    }
}
