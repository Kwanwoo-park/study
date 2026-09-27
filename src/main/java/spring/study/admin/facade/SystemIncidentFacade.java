package spring.study.admin.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.admin.dto.IncidentRequestContext;
import spring.study.admin.service.SystemIncidentService;
import spring.study.common.service.ClientIpResolver;

@Service
@RequiredArgsConstructor
public class SystemIncidentFacade {
    private final SystemIncidentService systemIncidentService;

    public void record(HttpServletRequest request, Exception exception) {
        systemIncidentService.record(new IncidentRequestContext(request.getMethod(), request.getRequestURI(),
                ClientIpResolver.resolve(request)), exception);
    }
}
