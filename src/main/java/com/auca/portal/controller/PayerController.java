package com.auca.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/payer")
public class PayerController {
    
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayer(@RequestBody Map<String, String> request) {
        String merchantCode = request.get("merchant_code");
        String payerCode = request.get("payer_code");
        
        // TODO: Query AUCA database for student fee info
        // Ask AUCA IT for the exact table and columns
        
        // For now, return mock data - Replace with actual AUCA database query
        Map<String, Object> response = new HashMap<>();
        response.put("payer_name", "Student " + payerCode);
        response.put("services", Arrays.asList(
            Map.of("service_name", "Tuition Fees", "amount", 500000),
            Map.of("service_name", "Registration", "amount", 50000)
        ));
        response.put("full_amount_required", false);
        
        return ResponseEntity.ok(response);
    }
}