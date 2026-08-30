package spring.study.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void directPublicRequestIgnoresSpoofedForwardingHeaders() {
        MockHttpServletRequest request = request("198.51.100.20");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("X-Real-IP", "5.6.7.8");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void trustedPrivateProxyCanForwardClientAddress() {
        MockHttpServletRequest request = request("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void resolverWalksProxyChainFromRightAndSkipsTrustedHops() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.8, 10.0.0.20");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.8");
    }

    @Test
    void invalidForwardedAddressFallsBackToRealIpForTrustedProxy() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "203.0.113.9");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
