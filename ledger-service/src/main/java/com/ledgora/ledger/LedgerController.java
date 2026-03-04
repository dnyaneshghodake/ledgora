package com.ledgora.ledger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerController {

    @GetMapping("/ledger/status")
    public String status() {
        return "ledger-service OK";
    }
}

