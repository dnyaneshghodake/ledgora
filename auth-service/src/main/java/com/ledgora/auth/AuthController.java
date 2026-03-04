package com.ledgora.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/auth/status")
    public String status() {
        return "auth-service OK";
    }
}

