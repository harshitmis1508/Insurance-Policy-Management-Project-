package com.harshit.monocept.repository;

import com.harshit.monocept.entity.Claim;
import com.harshit.monocept.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
	Page<Claim> findByPolicyCustomerId(Long customerId, Pageable pageable);

	Page<Claim> findByClaimStatus(ClaimStatus status, Pageable pageable);

	boolean existsByClaimNumber(String claimNumber);
}