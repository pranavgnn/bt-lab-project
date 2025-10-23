package com.bt.accounts.client;

import com.bt.accounts.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "fd-calculator-service", url = "${services.fdcalculator.url}")
public interface FdCalculatorServiceClient {

    @PostMapping("/api/fd/calculate")
    ApiResponse<FdCalculationDto> calculateFd(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/fd/status")
    ApiResponse<Map<String, Object>> getServiceHealth(
            @RequestHeader("Authorization") String token);
}
