package com.harshit.monocept.repository;

import com.harshit.monocept.entity.PremiumPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PremiumPayment, Long> {
	Page<PremiumPayment> findByPolicyId(Long policyId, Pageable pageable);

	boolean existsByTransactionReference(String ref);
}