package spring.study.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.account.entity.SavingsAutoTransfer;

public interface SavingAutoTransferRepository extends JpaRepository<SavingsAutoTransfer, String> {
}
