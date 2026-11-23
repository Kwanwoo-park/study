package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import spring.study.common.service.ClientIpResolver;

import java.util.List;

@Component
public class IpBlockClientAddress {
    private final List<IpAddressMatcher> trustedProxies;

    public IpBlockClientAddress(@Value("${security.ip-block.trusted-proxies:}") List<String> trustedProxies) {
        this.trustedProxies = trustedProxies.stream()
                .map(String::strip)
                .filter(proxy -> !proxy.isEmpty())
                .map(this::matcher)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String address = ClientIpResolver.resolve(request,
                candidate -> trustedProxies.stream().anyMatch(proxy -> proxy.matches(candidate)));
        return "UNKNOWN".equals(address) ? null : address;
    }

    private IpAddressMatcher matcher(String proxy) {
        String[] parts = proxy.split("/", 2);
        String address = ClientIpResolver.canonicalAddress(parts[0]);
        if (address == null) {
            throw new IllegalArgumentException("security.ip-block.trusted-proxies must contain literal IPs or CIDRs");
        }
        return new IpAddressMatcher(parts.length == 1 ? address : address + "/" + parts[1]);
    }
}
