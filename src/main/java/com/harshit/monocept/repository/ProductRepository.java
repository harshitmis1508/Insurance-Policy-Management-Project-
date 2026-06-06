package com.harshit.monocept.repository;

import com.harshit.monocept.entity.InsuranceProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<InsuranceProduct, Long> {
	boolean existsByProductName(String productName);

	Page<InsuranceProduct> findByIsActiveTrue(Pageable pageable);
}