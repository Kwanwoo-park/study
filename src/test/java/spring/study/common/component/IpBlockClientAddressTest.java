package spring.study.common.component;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IpBlockClientAddressTest {
    @Test
    void directRequestsKeepTheConnectionAddress() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.12");

        assertThat(new IpBlockClientAddress(List.of()).resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void configuredProxyChainResolvesTheClientForEnforcement() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.22");
        request.addHeader("X-Forwarded-For", "203.0.113.12, 192.0.2.21");

        assertThat(new IpBlockClientAddress(List.of("192.0.2.0/24")).resolve(request)).isEqualTo("203.0.113.12");
    }
}
