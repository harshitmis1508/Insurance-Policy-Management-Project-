package com.harshit.monocept.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.harshit.monocept.dto.request.ProductRequest;
import com.harshit.monocept.dto.response.ProductResponse;
import com.harshit.monocept.entity.InsuranceProduct;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;

	// SRS FR-PRD-001: Admin product banaye
	public ProductResponse createProduct(ProductRequest req) {

		// SRS PRD-BR-001: duplicate name check
		if (productRepository.existsByProductName(req.getProductName()))
			throw new DuplicateResourceException("Product already exists: " + req.getProductName());

		InsuranceProduct product = InsuranceProduct.builder().productName(req.getProductName())
				.productType(req.getProductType()).description(req.getDescription())
				.isActive(req.getIsActive() != null ? req.getIsActive() : true).build();

		return mapToResponse(productRepository.save(product));
	}

	// SRS FR-PRD-002: Admin product update kare
	public ProductResponse updateProduct(Long id, ProductRequest req) {

		InsuranceProduct product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

		// SRS PRD-BR-001: name change hogi toh duplicate check
		if (!product.getProductName().equals(req.getProductName())
				&& productRepository.existsByProductName(req.getProductName()))
			throw new DuplicateResourceException("Product name already exists: " + req.getProductName());

		product.setProductName(req.getProductName());
		product.setProductType(req.getProductType());
		product.setDescription(req.getDescription());
		if (req.getIsActive() != null)
			product.setIsActive(req.getIsActive());

		return mapToResponse(productRepository.save(product));
	}

	// SRS FR-PRD-003: Deactivate — hard delete nahi (PRD-BR-003)
	public ProductResponse deactivateProduct(Long id) {
		InsuranceProduct product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

		// SRS PRD-BR-004
		if (!product.getIsActive())
			throw new BusinessRuleException("Product is already inactive");

		product.setIsActive(false);
		return mapToResponse(productRepository.save(product));
	}

	// SRS FR-PRD-004: Sabhi active products — public
	public Page<ProductResponse> getActiveProducts(Pageable pageable) {
		return productRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
	}

	// SRS FR-PRD-007: Admin saare products dekhe (active + inactive)
	public Page<ProductResponse> getAllProducts(Pageable pageable) {
		return productRepository.findAll(pageable).map(this::mapToResponse);
	}

	// Helper
	private ProductResponse mapToResponse(InsuranceProduct p) {
		return ProductResponse.builder().productId(p.getId()).productName(p.getProductName())
				.productType(p.getProductType()).description(p.getDescription()).isActive(p.getIsActive())
				.createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
	}
}