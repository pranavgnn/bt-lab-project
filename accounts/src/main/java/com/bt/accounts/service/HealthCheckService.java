package com.bt.accounts.service;

import com.bt.accounts.client.CustomerServiceClient;
import com.bt.accounts.client.FdCalculatorServiceClient;
import com.bt.accounts.client.ProductServiceClient;
import com.bt.accounts.dto.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final DataSource dataSource;
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;
    private final FdCalculatorServiceClient fdCalculatorServiceClient;

    public ServiceHealthResponse checkHealth(String authToken) {
        return ServiceHealthResponse.builder()
                .serviceName("Accounts Service")
                .status("UP")
                .version("1.0.0")
                .databaseConnected(checkDatabaseConnection())
                .customerServiceConnected(checkCustomerService(authToken))
                .productServiceConnected(checkProductService(authToken))
                .fdCalculatorServiceConnected(checkFdCalculatorService(authToken))
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private Boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.error("Database connection check failed", e);
            return false;
        }
    }

    private Boolean checkCustomerService(String authToken) {
        try {
            customerServiceClient.validateCustomer(authToken);
            return true;
        } catch (Exception e) {
            log.warn("Customer service health check failed", e);
            return false;
        }
    }

    private Boolean checkProductService(String authToken) {
        try {
            productServiceClient.getProductByCode("HEALTH_CHECK", authToken);
            return true;
        } catch (Exception e) {
            log.warn("Product service health check failed", e);
            return false;
        }
    }

    private Boolean checkFdCalculatorService(String authToken) {
        try {
            fdCalculatorServiceClient.getServiceHealth(authToken);
            return true;
        } catch (Exception e) {
            log.warn("FD Calculator service health check failed", e);
            return false;
        }
    }
}
