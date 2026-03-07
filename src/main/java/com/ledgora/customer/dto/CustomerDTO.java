package com.ledgora.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerDTO {
    private Long customerId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dob;
    private String nationalId;
    private String kycStatus;
    private String phone;
    private String email;
    private String address;
}
