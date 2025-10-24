package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.client.CustomerServiceClient;
import com.bt.fixeddeposit.client.ProductServiceClient;
import com.bt.fixeddeposit.dto.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;
    private final DataSource dataSource;

    public ServiceHealthResponse checkServiceHealth() {
        boolean customerServiceAvailable = checkCustomerService();
        boolean productServiceAvailable = checkProductService();
        String databaseStatus = checkDatabaseConnection();

        String overallStatus = (customerServiceAvailable && productServiceAvailable && "UP".equals(databaseStatus))
                ? "HEALTHY"
                : "DEGRADED";

        return ServiceHealthResponse.builder()
                .serviceName("FD Calculator Service")
                .status(overallStatus)
                .customerServiceAvailable(customerServiceAvailable)
                .productServiceAvailable(productServiceAvailable)
                .databaseStatus(databaseStatus)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private boolean checkCustomerService() {
        try {
            customerServiceClient.validateCustomer("Bearer dummy-token");
            return true;
        } catch (Exception e) {
            log.warn("Customer service health check failed", e);
            return false;
        }
    }

    private boolean checkProductService() {
        try {
            productServiceClient.getProductByCode("DUMMY", "Bearer dummy-token");
            return true;
        } catch (Exception e) {
            log.warn("Product service health check failed", e);
            return false;
        }
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.error("Database connection check failed", e);
            return "DOWN";
        }
    }
}
