package com.harshit.monocept.repository;

import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
	Optional<Policy> findByPolicyNumber(String policyNumber);
	
	boolean existsByPolicyNumber(String policyNumber);

	Page<Policy> findByCustomerId(Long customerId, Pageable pageable);

	Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);
}