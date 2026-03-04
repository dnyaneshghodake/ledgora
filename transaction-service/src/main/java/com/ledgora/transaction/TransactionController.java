package com.ledgora.transaction;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    @GetMapping("/transaction/status")
    public String status() {
        return "transaction-service OK";
    }
}

