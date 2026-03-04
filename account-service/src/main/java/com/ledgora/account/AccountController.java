package com.ledgora.account;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    @GetMapping("/account/status")
    public String status() {
        return "account-service OK";
    }
}
