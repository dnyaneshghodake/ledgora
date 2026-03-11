package com.ledgora.config.seeder;

import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.entity.CustomerTaxProfile;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.customer.repository.CustomerTaxProfileRepository;
import com.ledgora.tenant.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 10 — CBS CustomerMaster + Tax Profiles. Seeds verified/pending customers
 * with PAN, Aadhaar, GST tax profiles.
 */
@Component
public class CbsCustomerSeeder {

    private static final Logger log = LoggerFactory.getLogger(CbsCustomerSeeder.class);
    private final CustomerMasterRepository customerMasterRepository;
    private final CustomerTaxProfileRepository customerTaxProfileRepository;

    public CbsCustomerSeeder(
            CustomerMasterRepository customerMasterRepository,
            CustomerTaxProfileRepository customerTaxProfileRepository) {
        this.customerMasterRepository = customerMasterRepository;
        this.customerTaxProfileRepository = customerTaxProfileRepository;
    }

    public void seed(Tenant defaultTenant) {
        if (customerMasterRepository.count() > 0) {
            log.info("  [CBS Customers] CustomerMaster records already exist — skipping");
            return;
        }

        seedWithTax(
                defaultTenant,
                "CBS-CUST-001",
                "Rajesh",
                "Kumar",
                LocalDate.of(1985, 3, 15),
                "ABCDE1234F",
                "9100000001",
                "rajesh.kumar@email.com",
                "123 MG Road, Mumbai",
                "VERIFIED",
                "INDIVIDUAL",
                "123456789012",
                null);

        seedWithTax(
                defaultTenant,
                "CBS-CUST-002",
                "Priya",
                "Sharma",
                LocalDate.of(1990, 7, 22),
                "FGHIJ5678K",
                "9100000002",
                "priya.sharma@email.com",
                "456 Park Avenue, Delhi",
                "VERIFIED",
                "INDIVIDUAL",
                "987654321098",
                null);

        seedWithTax(
                defaultTenant,
                "CBS-CUST-003",
                "Acme",
                "Corp",
                null,
                "KLMNO9012P",
                "9100000003",
                "finance@acmecorp.com",
                "789 Business Park, Bangalore",
                "VERIFIED",
                "CORPORATE",
                null,
                "29KLMNO9012PZAB");

        // Pending customer (no tax profile)
        CustomerMaster cm4 =
                CustomerMaster.builder()
                        .tenant(defaultTenant)
                        .customerNumber("CBS-CUST-004")
                        .firstName("Amit")
                        .lastName("Patel")
                        .fullName("Amit Patel")
                        .dob(LocalDate.of(1988, 11, 5))
                        .nationalId("PQRST3456U")
                        .phone("9100000004")
                        .email("amit.patel@email.com")
                        .address("321 Hill Street, Hyderabad")
                        .kycStatus("PENDING")
                        .customerType("INDIVIDUAL")
                        .makerCheckerStatus(MakerCheckerStatus.PENDING)
                        .approvalStatus(MakerCheckerStatus.PENDING)
                        .build();
        customerMasterRepository.save(cm4);

        log.info(
                "  [CBS Customers] 4 CustomerMaster records seeded (3 APPROVED, 1 PENDING) with tax profiles");
    }

    private void seedWithTax(
            Tenant tenant,
            String custNo,
            String first,
            String last,
            LocalDate dob,
            String nationalId,
            String phone,
            String email,
            String address,
            String kycStatus,
            String custType,
            String aadhaar,
            String gst) {
        CustomerMaster cm =
                CustomerMaster.builder()
                        .tenant(tenant)
                        .customerNumber(custNo)
                        .firstName(first)
                        .lastName(last)
                        .fullName(first + " " + last)
                        .dob(dob)
                        .nationalId(nationalId)
                        .phone(phone)
                        .email(email)
                        .address(address)
                        .kycStatus(kycStatus)
                        .customerType(custType)
                        .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                        .approvalStatus(MakerCheckerStatus.APPROVED)
                        .build();
        cm = customerMasterRepository.save(cm);

        CustomerTaxProfile tp =
                CustomerTaxProfile.builder()
                        .tenant(tenant)
                        .customerMaster(cm)
                        .panNumber(nationalId)
                        .aadhaarNumber(aadhaar)
                        .gstNumber(gst)
                        .tdsApplicable(true)
                        .tdsRate(
                                "CORPORATE".equals(custType)
                                        ? new BigDecimal("2.00")
                                        : new BigDecimal("10.00"))
                        .fatcaDeclaration(!"CORPORATE".equals(custType))
                        .taxResidencyStatus("RESIDENT")
                        .taxDeductionFlag(true)
                        .build();
        tp = customerTaxProfileRepository.save(tp);
        cm.setTaxProfile(tp);
        customerMasterRepository.save(cm);
    }
}
