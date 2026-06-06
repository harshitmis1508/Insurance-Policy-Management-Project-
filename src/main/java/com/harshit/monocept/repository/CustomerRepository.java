package com.harshit.monocept.repository;

import com.harshit.monocept.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByUserId(Long userId);

	boolean existsByUserId(Long userId);
}