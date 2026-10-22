package com.campusmarket.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

	Page<WalletLedger> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
