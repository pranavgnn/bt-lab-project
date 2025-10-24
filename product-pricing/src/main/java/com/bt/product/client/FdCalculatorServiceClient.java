package com.bt.product.client;

import com.bt.product.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "fd-calculator-service", url = "${services.fdcalculator.url}")
public interface FdCalculatorServiceClient {

    @PostMapping("/api/fd/calculate")
    ApiResponse<FdCalculationDto> calculateFd(
            @RequestBody FdCalculationRequestDto request,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/fd/history/{customerId}")
    ApiResponse<List<FdCalculationDto>> getCalculationHistory(
            @PathVariable("customerId") Long customerId,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/fd/status")
    ServiceHealthDto getServiceHealth();
}
