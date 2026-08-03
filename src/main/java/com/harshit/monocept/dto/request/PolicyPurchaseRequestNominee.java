package com.harshit.monocept.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPurchaseRequestNominee {
    @NotBlank
    private String name;

    @NotBlank
    private String relationship; // SPOUSE | PARENT | CHILD | OTHER

    @Min(1)
    @Max(100)
    private Integer sharePct; // total across nominees should be 100 (validated in service)

    private LocalDate dob; // optional
}
