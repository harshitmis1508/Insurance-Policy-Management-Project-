package com.harshit.monocept.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.harshit.monocept.entity.Claim;
import com.harshit.monocept.enums.ClaimStatus;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
	Page<Claim> findByPolicyCustomerId(Long customerId, Pageable pageable);

	Page<Claim> findByClaimStatus(ClaimStatus status, Pageable pageable);

	Page<Claim> findByAssignedAgentId(Long agentId, Pageable pageable);

	long countByPolicyCustomerIdAndIdNot(Long customerId, Long claimId);

	boolean existsByClaimNumber(String claimNumber);
}