package spring.study.common.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.study.admin.service.IpBlockService;
import spring.study.common.service.ClientIpResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
@RequiredArgsConstructor
public class IpBlockFilter extends OncePerRequestFilter {
    private final IpBlockClientAddress clientAddress;
    private final IpBlockService blocks;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String address = clientAddress.resolve(request);
        try {
            if (blocks.isBlocked(address)) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "차단된 IP입니다.");
                return;
            }
        } catch (DataAccessException | TransactionException failure) {
            reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "접속 허용 여부를 확인할 수 없습니다.");
            return;
        }
        if (address != null) request.setAttribute(ClientIpResolver.RESOLVED_ADDRESS, address);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Map.of("result", -status, "message", message));
    }
}
