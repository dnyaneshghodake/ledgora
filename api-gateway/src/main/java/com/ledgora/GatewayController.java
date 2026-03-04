// ...existing code...
package com.ledgora;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {

    @GetMapping("/gateway/status")
    public String status() {
        return "api-gateway OK";
    }
}

// ...existing code...

