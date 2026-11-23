package spring.study.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.entity.BlockedIp;
import spring.study.admin.repository.BlockedIpRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IpBlockService {
    private final BlockedIpRepository repository;

    public boolean isBlocked(String canonicalAddress) {
        return canonicalAddress != null && repository.existsById(canonicalAddress);
    }

    public boolean permits(String canonicalAddress) {
        try {
            return !isBlocked(canonicalAddress);
        } catch (DataAccessException | TransactionException failure) {
            return false;
        }
    }

    @Transactional
    public void block(String canonicalAddress, Long administratorId) {
        if (repository.findById(canonicalAddress).isEmpty()) {
            repository.saveAndFlush(new BlockedIp(canonicalAddress, administratorId));
        }
    }

    public List<BlockedIp> list() {
        return repository.findAllByOrderByBlockedAtDescIpAddressAsc();
    }
}
