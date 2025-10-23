package com.bt.customer.client;

import com.bt.customer.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/product/{id}")
    ApiResponse<ProductDto> getProductById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/product/code/{productCode}")
    ApiResponse<ProductDto> getProductByCode(
            @PathVariable("productCode") String productCode,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/product/search")
    ApiResponse<List<ProductDto>> searchProducts(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String token);
}
