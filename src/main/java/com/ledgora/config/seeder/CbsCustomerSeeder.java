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
                // RBI KYC: PAN 4th character must be 'C' for Company/Corporate entities
                "KLMCO9012P",
                "9100000003",
                "finance@acmecorp.com",
                "789 Business Park, Bangalore",
                "VERIFIED",
                "CORPORATE",
                null,
                // GST format: 2-digit state + 10-char PAN + entity code + Z + checksum
                "29KLMCO9012PCZB");

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

    /**
     * Seeds realistic customers for additional tenants (T2–T5). Each tenant gets 4–6 customers with
     * Indian names matching the tenant's geography.
     */
    public void seedForAdditionalTenants(Tenant t2, Tenant t3, Tenant t4, Tenant t5) {

        // ── Tenant 2: Partner Bank (Pune) ──
        if (t2 != null) {
            seedWithTax(
                    t2,
                    "T2-CUST-001",
                    "Suhas",
                    "Joshi",
                    LocalDate.of(1982, 5, 10),
                    "AAJPJ1234A",
                    "9200100001",
                    "suhas.joshi@email.com",
                    "12 FC Road, Deccan, Pune",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "412345678901",
                    null);
            seedWithTax(
                    t2,
                    "T2-CUST-002",
                    "Manisha",
                    "Deshpande",
                    LocalDate.of(1991, 9, 18),
                    "BBKPD5678B",
                    "9200100002",
                    "manisha.d@email.com",
                    "45 Karve Nagar, Pune",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "412345678902",
                    null);
            seedWithTax(
                    t2,
                    "T2-CUST-003",
                    "Hinjewadi",
                    "Tech Park Pvt Ltd",
                    null,
                    "AACCH9012C",
                    "9200100003",
                    "accounts@hinjewadi-tech.com",
                    "Phase 1, Hinjewadi IT Park, Pune",
                    "VERIFIED",
                    "CORPORATE",
                    null,
                    "27AACCH9012CCZD");
            seedWithTax(
                    t2,
                    "T2-CUST-004",
                    "Nitin",
                    "Kulkarni",
                    LocalDate.of(1975, 1, 25),
                    "CCLPK3456D",
                    "9200100004",
                    "nitin.k@email.com",
                    "78 MG Road, Camp, Pune",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "412345678903",
                    null);
            log.info("  [CBS Customers] T2: 4 customers seeded (Partner Bank — Pune)");
        }

        // ── Tenant 3: Sahyadri UCB (Pune — cooperative members) ──
        if (t3 != null) {
            seedWithTax(
                    t3,
                    "T3-CUST-001",
                    "Dattatray",
                    "Patil",
                    LocalDate.of(1970, 3, 8),
                    "DDMPD1234E",
                    "9300100001",
                    "dattatray.patil@email.com",
                    "15 Sadashiv Peth, Pune",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "412345679001",
                    null);
            seedWithTax(
                    t3,
                    "T3-CUST-002",
                    "Vaishali",
                    "Kadam",
                    LocalDate.of(1985, 12, 20),
                    "EENPK5678F",
                    "9300100002",
                    "vaishali.k@email.com",
                    "23 Narayan Peth, Pune",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "412345679002",
                    null);
            seedWithTax(
                    t3,
                    "T3-CUST-003",
                    "Sahyadri",
                    "Dairy Cooperative",
                    null,
                    "FFQSC9012G",
                    "9300100003",
                    "sahyadri.dairy@email.com",
                    "Parvati, Pune",
                    "VERIFIED",
                    "CORPORATE",
                    null,
                    "27FFQSC9012GCZH");
            log.info("  [CBS Customers] T3: 3 customers seeded (Sahyadri UCB — cooperative)");
        }

        // ── Tenant 4: Maharashtra Gramin Bank (Nashik — rural farmers + MSME) ──
        if (t4 != null) {
            seedWithTax(
                    t4,
                    "T4-CUST-001",
                    "Shankar",
                    "Pawar",
                    LocalDate.of(1965, 6, 14),
                    "GGRPP1234H",
                    "9400100001",
                    "shankar.pawar@email.com",
                    "Village Ozar, Taluka Sinnar, Nashik",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "422345670001",
                    null);
            seedWithTax(
                    t4,
                    "T4-CUST-002",
                    "Suman",
                    "Jadhav",
                    LocalDate.of(1980, 4, 2),
                    "HHSPJ5678I",
                    "9400100002",
                    "suman.jadhav@email.com",
                    "Village Ghoti, Taluka Igatpuri, Nashik",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "422345670002",
                    null);
            seedWithTax(
                    t4,
                    "T4-CUST-003",
                    "Nashik",
                    "Grape Farmers FPO",
                    null,
                    "IITCN9012J",
                    "9400100003",
                    "nashik.grape.fpo@email.com",
                    "APMC Yard, Nashik",
                    "VERIFIED",
                    "CORPORATE",
                    null,
                    "27IITCN9012JCZK");
            seedWithTax(
                    t4,
                    "T4-CUST-004",
                    "Bhagwan",
                    "Gaikwad",
                    LocalDate.of(1972, 8, 30),
                    "JJUPG3456K",
                    "9400100004",
                    "bhagwan.g@email.com",
                    "Dindori Taluka, Nashik",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "422345670003",
                    null);
            seedWithTax(
                    t4,
                    "T4-CUST-005",
                    "Sunanda",
                    "More",
                    LocalDate.of(1988, 2, 15),
                    "KKVQM7890L",
                    "9400100005",
                    "sunanda.more@email.com",
                    "Sinnar Town, Nashik",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "422345670004",
                    null);
            log.info("  [CBS Customers] T4: 5 customers seeded (Gramin Bank — rural Nashik)");
        }

        // ── Tenant 5: Finserv Capital NBFC (metro — salaried + MSME borrowers) ──
        if (t5 != null) {
            seedWithTax(
                    t5,
                    "T5-CUST-001",
                    "Vikram",
                    "Malhotra",
                    LocalDate.of(1983, 10, 5),
                    "LLWRM1234M",
                    "9500100001",
                    "vikram.m@email.com",
                    "BKC, Bandra East, Mumbai",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "400345680001",
                    null);
            seedWithTax(
                    t5,
                    "T5-CUST-002",
                    "Deepa",
                    "Nair",
                    LocalDate.of(1992, 7, 12),
                    "MMXSN5678N",
                    "9500100002",
                    "deepa.nair@email.com",
                    "Koramangala, Bengaluru",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "560345680002",
                    null);
            seedWithTax(
                    t5,
                    "T5-CUST-003",
                    "NovaTech",
                    "Solutions Pvt Ltd",
                    null,
                    "NNYTN9012O",
                    "9500100003",
                    "finance@novatech.com",
                    "Sector 44, Gurugram",
                    "VERIFIED",
                    "CORPORATE",
                    null,
                    "06NNYTN9012OCZP");
            seedWithTax(
                    t5,
                    "T5-CUST-004",
                    "Arjun",
                    "Reddy",
                    LocalDate.of(1987, 11, 28),
                    "OOZUR3456P",
                    "9500100004",
                    "arjun.reddy@email.com",
                    "HITEC City, Hyderabad",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "500345680003",
                    null);
            seedWithTax(
                    t5,
                    "T5-CUST-005",
                    "Priya",
                    "Menon",
                    LocalDate.of(1995, 1, 9),
                    "PPAWM7890Q",
                    "9500100005",
                    "priya.menon@email.com",
                    "Indiranagar, Bengaluru",
                    "VERIFIED",
                    "INDIVIDUAL",
                    "560345680004",
                    null);
            seedWithTax(
                    t5,
                    "T5-CUST-006",
                    "Metro",
                    "Retail Chain LLP",
                    null,
                    "QQBXR1234R",
                    "9500100006",
                    "accounts@metro-retail.com",
                    "Lower Parel, Mumbai",
                    "VERIFIED",
                    "CORPORATE",
                    null,
                    "27QQBXR1234RCZS");
            log.info("  [CBS Customers] T5: 6 customers seeded (Finserv NBFC — metro borrowers)");
        }
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
