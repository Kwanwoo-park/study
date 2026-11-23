package spring.study.admin.repository;

import org.springframework.data.repository.Repository;
import spring.study.admin.entity.BlockedIp;

import java.util.List;
import java.util.Optional;

public interface BlockedIpRepository extends Repository<BlockedIp, String> {
    boolean existsById(String ipAddress);

    Optional<BlockedIp> findById(String ipAddress);

    BlockedIp saveAndFlush(BlockedIp block);

    List<BlockedIp> findAllByOrderByBlockedAtDescIpAddressAsc();
}
