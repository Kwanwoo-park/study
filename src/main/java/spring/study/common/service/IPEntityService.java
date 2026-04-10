package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.common.dto.IPEntityRequestDto;
import spring.study.common.entity.IPEntity;
import spring.study.common.repository.IPEntityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPEntityService {
    private final IPEntityRepository ipEntityRepository;

    @Transactional
    public IPEntity save(IPEntity ipEntity) {
        return ipEntityRepository.save(ipEntity);
    }

    @Transactional
    public IPEntity save(IPEntityRequestDto dto) {
        return ipEntityRepository.save(dto.toEntity());
    }

    public List<IPEntity> findAll() {
        return ipEntityRepository.findAll();
    }

    public List<IPEntity> findByMemberId(Long memberId) {
        return ipEntityRepository.findByMemberId(memberId);
    }

    public IPEntity findByIp(String ip) {
        return ipEntityRepository.findByIp(ip);
    }

    public Boolean exist(String ip, Long memberId) {
        return ipEntityRepository.existsByIpAndMemberId(ip, memberId);
    }

    public String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_CLIENT-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_X-FORWARDED_FOR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-RealIP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("REMOTE_ADDR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();

        return ip.split(",")[0];
    }

    @Transactional
    public void updateIP(Long id, String ip) {
        IPEntity ipEntity = ipEntityRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 정보입니다"
        ));

        ipEntity.setIp(ip);
    }

    public void deleteIp(String ip) {
        ipEntityRepository.deleteByIp(ip);
    }

    public void deleteMemberId(Long memberId) {
        ipEntityRepository.deleteByMemberId(memberId);
    }
}
