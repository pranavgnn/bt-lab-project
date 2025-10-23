package com.bt.product.client;

import com.bt.product.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "customer-service", url = "${services.customer.url}")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{id}")
    ApiResponse<CustomerDto> getCustomerById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/customers/validate")
    ApiResponse<Boolean> validateCustomer(
            @RequestHeader("Authorization") String token);
}
