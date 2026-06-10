package com.harshit.monocept.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claim_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claim_id", nullable = false)
	private Claim claim;

	@Column(nullable = false)
	private String documentName;

	@Column(nullable = false)
	private String documentType;

	// SRS DOC-BR-003: Ab actual Cloudinary URL store hogi
	@Column(nullable = false)
	private String documentReference; // Cloudinary URL

	// Cloudinary public ID — delete ke liye zaroori
	@Column
	private String cloudinaryPublicId;

	// File ka original naam
	@Column
	private String originalFileName;

	// File size bytes mein
	@Column
	private Long fileSizeBytes;

	// File format — pdf, jpg etc
	@Column
	private String fileFormat;

	private LocalDateTime uploadedAt;

	@PrePersist
	protected void onCreate() {
		uploadedAt = LocalDateTime.now();
	}
}