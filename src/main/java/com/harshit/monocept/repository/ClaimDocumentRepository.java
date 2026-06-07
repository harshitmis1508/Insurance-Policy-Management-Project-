package com.harshit.monocept.repository;

import com.harshit.monocept.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
	List<ClaimDocument> findByClaimId(Long claimId);
}