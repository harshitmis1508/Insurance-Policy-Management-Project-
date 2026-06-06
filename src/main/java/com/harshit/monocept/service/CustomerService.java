package com.harshit.monocept.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.harshit.monocept.dto.request.CustomerRequest;
import com.harshit.monocept.dto.response.CustomerResponse;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	// SRS FR-CUS-001: Customer apna profile create kare
	public CustomerResponse createProfile(CustomerRequest req, String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// SRS CUS-BR-002: ek user ka sirf ek profile
		if (customerRepository.existsByUserId(user.getId()))
			throw new BusinessRuleException("Profile already exists for this user");

		Customer customer = Customer.builder().user(user).dateOfBirth(req.getDateOfBirth()).address(req.getAddress())
				.city(req.getCity()).state(req.getState()).pinCode(req.getPinCode()).nomineeName(req.getNomineeName())
				.nomineeRelation(req.getNomineeRelation()).build();

		return mapToResponse(customerRepository.save(customer));
	}

	// SRS FR-CUS-002: Customer apna profile update kare
	public CustomerResponse updateProfile(CustomerRequest req, String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found. Create profile first."));

		// SRS CUS-BR-004: Customer sirf apna profile update kare
		customer.setDateOfBirth(req.getDateOfBirth());
		customer.setAddress(req.getAddress());
		customer.setCity(req.getCity());
		customer.setState(req.getState());
		customer.setPinCode(req.getPinCode());
		customer.setNomineeName(req.getNomineeName());
		customer.setNomineeRelation(req.getNomineeRelation());

		return mapToResponse(customerRepository.save(customer));
	}

	// SRS FR-CUS-003: Customer apna profile dekhe
	public CustomerResponse getMyProfile(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

		return mapToResponse(customer);
	}

	// SRS FR-CUS-004: Admin/Agent kisi bhi customer ka profile dekhe
	public CustomerResponse getCustomerById(Long customerId) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
		return mapToResponse(customer);
	}

	// SRS FR-CUS-006: Paginated customer listing for admin/agent
	public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
		return customerRepository.findAll(pageable).map(this::mapToResponse);
	}

	// Helper: Entity → Response DTO
	private CustomerResponse mapToResponse(Customer c) {
		return CustomerResponse.builder().customerId(c.getId()).fullName(c.getUser().getFullName())
				.email(c.getUser().getEmail()).mobileNumber(c.getUser().getMobileNumber())
				.dateOfBirth(c.getDateOfBirth()).address(c.getAddress()).city(c.getCity()).state(c.getState())
				.pinCode(c.getPinCode()).nomineeName(c.getNomineeName()).nomineeRelation(c.getNomineeRelation())
				.createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
	}
}