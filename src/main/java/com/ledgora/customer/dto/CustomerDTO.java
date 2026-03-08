package com.ledgora.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerDTO {
    private Long customerId;

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[A-Za-z .]+$", message = "Name must contain only alphabets, spaces, and dots")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[A-Za-z .]+$", message = "Name must contain only alphabets, spaces, and dots")
    private String lastName;

    private LocalDate dob;
    private String nationalId;
    private String kycStatus;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile must be exactly 10 digits")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    // RBI-grade fields
    private String customerType;       // INDIVIDUAL or CORPORATE
    private String panNumber;
    private String aadhaarNumber;
    private String gstNumber;
    private String freezeLevel;
    private String freezeReason;
    private String approvalStatus;
    private String riskCategory;
}
