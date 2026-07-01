package com.harshit.monocept.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.enums.PolicyStatus;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
	Optional<Policy> findByPolicyNumber(String policyNumber);

	boolean existsByPolicyNumber(String policyNumber);

	Page<Policy> findByCustomerId(Long customerId, Pageable pageable);

	Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);
}