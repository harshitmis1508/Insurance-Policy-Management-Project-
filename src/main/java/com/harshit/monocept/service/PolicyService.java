package com.harshit.monocept.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.dto.request.PolicyIssueRequest;
import com.harshit.monocept.dto.request.PolicyPurchaseRequest;
import com.harshit.monocept.dto.response.PolicyResponse;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.entity.PolicyPlan;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.enums.PremiumFrequency;
import com.harshit.monocept.enums.PremiumType;
import com.harshit.monocept.enums.ProductType;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.PolicyPlanRepository;
import com.harshit.monocept.repository.PolicyRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private static final Logger log = LoggerFactory.getLogger(PolicyService.class);
	private static final Pattern VEHICLE_REG_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$");
	private static final int MAX_VEHICLE_AGE_YEARS = 15;
	private static final int MAX_TRAVEL_ADVANCE_DAYS = 90;

	private final PolicyRepository policyRepository;
	private final PolicyPlanRepository planRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	private BigDecimal getDepreciationPercent(int vehicleAgeYears) {
		if (vehicleAgeYears <= 0)
			return new BigDecimal("0.05");
		if (vehicleAgeYears == 1)
			return new BigDecimal("0.15");
		if (vehicleAgeYears == 2)
			return new BigDecimal("0.20");
		if (vehicleAgeYears == 3)
			return new BigDecimal("0.30");
		if (vehicleAgeYears == 4)
			return new BigDecimal("0.40");

		return new BigDecimal("0.50");
	}

	private BigDecimal calculateIdv(BigDecimal baseVehicleValue, int vehicleAgeYears) {
		BigDecimal depreciationPct = getDepreciationPercent(vehicleAgeYears);
		BigDecimal depreciationAmount = baseVehicleValue.multiply(depreciationPct);

		return baseVehicleValue.subtract(depreciationAmount).setScale(2, RoundingMode.HALF_UP);
	}

	@Transactional
	public PolicyResponse purchasePolicy(PolicyPurchaseRequest req, String email) {
		log.info("Policy purchase attempt: email={}, planId={}", email, req.getPlanId());

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId()).orElseThrow(() -> {
			log.warn("Policy purchase without profile: email={}", email);
			return new BusinessRuleException("Please complete your profile before purchasing a policy");
		});

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		if (!plan.getIsActive()) {
			log.warn("Purchase attempt on inactive plan: planId={}", req.getPlanId());
			throw new BusinessRuleException("Cannot purchase an inactive plan");
		}

		if (!plan.getProduct().getIsActive()) {
			log.warn("Purchase attempt on inactive product: productId={}", plan.getProduct().getId());
			throw new BusinessRuleException("Cannot purchase plan of an inactive product");
		}
		List<PolicyStatus> activeLikeStatuses = List.of(PolicyStatus.PENDING_PAYMENT, PolicyStatus.ACTIVE,
				PolicyStatus.LAPSED);

		if (plan.getProduct().getProductType() == ProductType.MOTOR) {

			String regNo = req.getVehicleRegistrationNumber().trim().toUpperCase().replaceAll("\\s+", "");

			boolean vehicleAlreadyInsured = policyRepository.existsByVehicleRegistrationNumberAndStatusIn(regNo,
					activeLikeStatuses);

			if (vehicleAlreadyInsured) {
				throw new BusinessRuleException("This vehicle is already insured under an active or pending policy");
			}

		} else {

			boolean hasDuplicate = !policyRepository
					.findByCustomerIdAndPlanIdAndStatusIn(customer.getId(), plan.getId(), activeLikeStatuses).isEmpty();

			if (hasDuplicate) {
				throw new BusinessRuleException("You already have an active or pending policy for this plan");
			}
		}

		int pendingCount = policyRepository.countByCustomerIdAndStatus(customer.getId(), PolicyStatus.PENDING_PAYMENT);
		if (pendingCount >= 2) {
			throw new BusinessRuleException(
					"You already have " + pendingCount + " pending payments. Complete them before buying another plan");
		}

		int sameProductCount = policyRepository.countByCustomerIdAndPlanProductProductTypeAndStatusIn(customer.getId(),
				plan.getProduct().getProductType(), activeLikeStatuses);
		int limit = plan.getProduct().getProductType() == ProductType.LIFE ? 1
				: plan.getProduct().getProductType() == ProductType.HEALTH ? 2 : Integer.MAX_VALUE;
		if (sameProductCount >= limit) {
			throw new BusinessRuleException("You've reached the maximum number of active "
					+ plan.getProduct().getProductType() + " policies (" + limit + ") allowed for standard customers");
		}

		Policy policy = policyRepository.save(buildPolicy(customer, plan, req.getStartDate(), req.getPremiumFrequency(),
				req.getVehicleRegistrationNumber(), req.getVehicleMake(), req.getVehicleModel(),
				req.getVehicleManufactureYear()));

		log.info("Policy purchased: policyNumber={}, customer={}, planId={}, frequency={}", policy.getPolicyNumber(),
				email, req.getPlanId(), policy.getPremiumFrequency());

		return mapToResponse(policy);
	}

	@Transactional
	public PolicyResponse issuePolicy(PolicyIssueRequest req) {
		log.info("Policy issue attempt: customerId={}, planId={}", req.getCustomerId(), req.getPlanId());

		Customer customer = customerRepository.findById(req.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + req.getCustomerId()));

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		if (!plan.getIsActive()) {
			log.warn("Issue attempt on inactive plan: planId={}", req.getPlanId());
			throw new BusinessRuleException("Cannot issue an inactive plan");
		}

		if (!plan.getProduct().getIsActive()) {
			log.warn("Issue attempt on inactive product: productId={}", plan.getProduct().getId());
			throw new BusinessRuleException("Cannot issue plan of an inactive product");
		}

		List<PolicyStatus> activeLikeStatuses = List.of(PolicyStatus.PENDING_PAYMENT, PolicyStatus.ACTIVE,
				PolicyStatus.LAPSED);

		if (plan.getProduct().getProductType() == ProductType.MOTOR) {

			String regNo = req.getVehicleRegistrationNumber().trim().toUpperCase().replaceAll("\\s+", "");

			if (policyRepository.existsByVehicleRegistrationNumberAndStatusIn(regNo, activeLikeStatuses)) {

				throw new BusinessRuleException("This vehicle is already insured under an active or pending policy");
			}
		}

		Policy policy = policyRepository.save(buildPolicy(customer, plan, req.getStartDate(), req.getPremiumFrequency(),
				req.getVehicleRegistrationNumber(), req.getVehicleMake(), req.getVehicleModel(),
				req.getVehicleManufactureYear()));

		// SRS LOG-007: Policy issuance log
		log.info("Policy issued: policyNumber={}, customerId={}, planId={}, frequency={}", policy.getPolicyNumber(),
				req.getCustomerId(), req.getPlanId(), policy.getPremiumFrequency());

		return mapToResponse(policy);
	}

	public Page<PolicyResponse> getMyPolicies(String email, Pageable pageable) {
		log.debug("Fetching policies for: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		return policyRepository.findByCustomerId(customer.getId(), pageable).map(this::mapToResponse);
	}

	public Page<PolicyResponse> getAllPolicies(Pageable pageable) {
		log.debug("Fetching all policies, page: {}", pageable.getPageNumber());
		return policyRepository.findAll(pageable).map(this::mapToResponse);
	}

	public Page<PolicyResponse> getPoliciesByCustomer(Long customerId, Pageable pageable) {
		log.debug("Fetching policies for customerId: {}", customerId);

		customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

		return policyRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
	}

	@Transactional
	public PolicyResponse cancelPolicy(Long policyId, String email) {
		log.info("Policy cancel attempt: policyId={}, by={}", policyId, email);

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		if (policy.getStatus() == PolicyStatus.CANCELLED) {
			log.warn("Already cancelled policy: policyId={}", policyId);
			throw new BusinessRuleException("Policy is already cancelled");
		}

		if (policy.getStatus() == PolicyStatus.EXPIRED) {
			log.warn("Cancel attempt on expired policy: policyId={}", policyId);
			throw new BusinessRuleException("Expired policy cannot be cancelled");
		}

		policy.setStatus(PolicyStatus.CANCELLED);
		Policy saved = policyRepository.save(policy);
		log.info("Policy cancelled: policyNumber={}", saved.getPolicyNumber());

		return mapToResponse(saved);
	}

	private void validateStartDate(PolicyPlan plan, LocalDate startDate) {
		LocalDate today = LocalDate.now();

		if (plan.getProduct().getProductType() == ProductType.TRAVEL) {
			if (startDate.isAfter(today.plusDays(MAX_TRAVEL_ADVANCE_DAYS))) {
				throw new BusinessRuleException(
						"Trip start date cannot be more than " + MAX_TRAVEL_ADVANCE_DAYS + " days from today");
			}
		} else {
			if (!startDate.isEqual(today)) {
				throw new BusinessRuleException("This policy's coverage must start today (" + today
						+ "). Only travel policies can have a future start date");
			}
		}
	}

	public PolicyResponse getPolicyById(Long policyId, String email) {
		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.getRole() == Role.CUSTOMER) {
			Customer customer = customerRepository.findByUserId(user.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
			if (!policy.getCustomer().getId().equals(customer.getId())) {
				log.warn("Customer {} attempted to access policyId={} owned by customerId={}", email, policyId,
						policy.getCustomer().getId());
				throw new BusinessRuleException("You can only access your own policies");
			}
		}

		if (policy.getStatus() == PolicyStatus.ACTIVE && policy.getEndDate().isBefore(LocalDate.now())) {
			policy.setStatus(PolicyStatus.EXPIRED);
			policyRepository.save(policy);
			log.info("Policy auto-expired: policyNumber={}", policy.getPolicyNumber());
		}

		return mapToResponse(policy);
	}

	private String generatePolicyNumber() {
		String number;
		do {
			number = "POL-" + System.currentTimeMillis() + "-"
					+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		} while (policyRepository.existsByPolicyNumber(number));
		return number;
	}

	private Policy buildPolicy(Customer customer, PolicyPlan plan, LocalDate startDate,
			PremiumFrequency requestedFrequency, String vehicleRegNo, String vehicleMake, String vehicleModel,
			Integer vehicleYear) {

		validateStartDate(plan, startDate);

		String cleanRegNo = null;
		BigDecimal idv = null;
		Integer vehicleAge = null;
		Integer depreciationPct = null;
	
		if (plan.getProduct().getProductType() == ProductType.MOTOR) {
			cleanRegNo = validateVehicleDetails(vehicleRegNo, vehicleMake, vehicleModel, vehicleYear);
			vehicleAge = Year.now().getValue() - vehicleYear;
			idv = calculateIdv(plan.getCoverageAmount(), vehicleAge);
			depreciationPct = getDepreciationPercent(vehicleAge).multiply(BigDecimal.valueOf(100)).intValue();
		}

		PremiumFrequency frequency = null;
		BigDecimal installmentAmount;
		Integer totalInstallmentsDue;

		if (plan.getPremiumType() == PremiumType.ONE_TIME) {
			installmentAmount = plan.getPremiumAmount();
			totalInstallmentsDue = 1;
		} else {
			if (requestedFrequency == null) {
				throw new BusinessRuleException(
						"Please select a premium payment frequency (Monthly / Quarterly / Half-Yearly / Annual) for this plan");
			}
			frequency = requestedFrequency;
			installmentAmount = calculateInstallmentAmount(plan.getPremiumAmount(), frequency);
			totalInstallmentsDue = frequency.getInstallmentsPerYear() * plan.getDurationYears();
		}

		Policy.PolicyBuilder builder = Policy.builder().policyNumber(generatePolicyNumber()).customer(customer)
				.plan(plan).startDate(startDate).endDate(startDate.plusYears(plan.getDurationYears()))
				.status(PolicyStatus.PENDING_PAYMENT).premiumsPaid(0).premiumFrequency(frequency)
				.installmentAmount(installmentAmount).totalInstallmentsDue(totalInstallmentsDue)
				.vehicleRegistrationNumber(cleanRegNo).vehicleMake(vehicleMake != null ? vehicleMake.trim() : null)
				.vehicleModel(vehicleModel != null ? vehicleModel.trim() : null)
				.vehicleManufactureYear(plan.getProduct().getProductType() == ProductType.MOTOR ? vehicleYear : null)
				.calculatedIdv(idv).vehicleAgeAtPurchase(vehicleAge).depreciationPercentApplied(depreciationPct);

		if (plan.getPremiumType() == PremiumType.ANNUAL) {
			builder.nextPremiumDueDate(startDate);
		}

		return builder.build();
	}

	private String validateVehicleDetails(String regNo, String make, String model, Integer year) {
		if (regNo == null || regNo.isBlank()) {
			throw new BusinessRuleException("Vehicle registration number is required for motor insurance");
		}
		if (make == null || make.isBlank()) {
			throw new BusinessRuleException("Vehicle make is required for motor insurance");
		}
		if (model == null || model.isBlank()) {
			throw new BusinessRuleException("Vehicle model is required for motor insurance");
		}
		if (year == null) {
			throw new BusinessRuleException("Vehicle manufacture year is required for motor insurance");
		}

		String cleanRegNo = regNo.trim().toUpperCase().replaceAll("\\s+", "");
		if (!VEHICLE_REG_PATTERN.matcher(cleanRegNo).matches()) {
			throw new BusinessRuleException(
					"Invalid vehicle registration number format. Expected format like DL01AB1234");
		}

		int currentYear = Year.now().getValue();
		if (year > currentYear) {
			throw new BusinessRuleException("Vehicle manufacture year cannot be in the future");
		}
		if (currentYear - year > MAX_VEHICLE_AGE_YEARS) {
			throw new BusinessRuleException("This vehicle is " + (currentYear - year)
					+ " years old. We only insure vehicles up to " + MAX_VEHICLE_AGE_YEARS + " years old");
		}

		return cleanRegNo;
	}

	// Core EMI formula: annual premium + frequency loading, split evenly across
	// installments
	private BigDecimal calculateInstallmentAmount(BigDecimal annualPremium, PremiumFrequency frequency) {
		BigDecimal loadedAnnual = annualPremium.multiply(BigDecimal.ONE.add(frequency.getLoadingFactor()));
		return loadedAnnual.divide(BigDecimal.valueOf(frequency.getInstallmentsPerYear()), 2, RoundingMode.HALF_UP);
	}

	public PolicyResponse mapToResponse(Policy p) {
		return PolicyResponse.builder().policyId(p.getId()).policyNumber(p.getPolicyNumber())
				.customerId(p.getCustomer().getId()).customerName(p.getCustomer().getUser().getFullName())
				.planId(p.getPlan().getId()).planName(p.getPlan().getPlanName())
				.productType(p.getPlan().getProduct().getProductType()).coverageAmount(p.getPlan().getCoverageAmount())
				.premiumAmount(p.getPlan().getPremiumAmount()).premiumType(p.getPlan().getPremiumType())
				.premiumFrequency(p.getPremiumFrequency()).installmentAmount(p.getInstallmentAmount())
				.totalInstallmentsDue(p.getTotalInstallmentsDue()).startDate(p.getStartDate()).endDate(p.getEndDate())
				.status(p.getStatus()).totalPremiumPaid(p.getTotalPremiumPaid()).premiumsPaid(p.getPremiumsPaid())
				.nextPremiumDueDate(p.getNextPremiumDueDate()).durationYears(p.getPlan().getDurationYears())
				.createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
				.vehicleRegistrationNumber(p.getVehicleRegistrationNumber()).vehicleMake(p.getVehicleMake())
				.vehicleModel(p.getVehicleModel()).vehicleManufactureYear(p.getVehicleManufactureYear())
				.calculatedIdv(p.getCalculatedIdv()).vehicleAgeAtPurchase(p.getVehicleAgeAtPurchase())
				.depreciationPercentApplied(p.getDepreciationPercentApplied()).ncbPercentage(p.getNcbPercentage())
				.build();
	}
}