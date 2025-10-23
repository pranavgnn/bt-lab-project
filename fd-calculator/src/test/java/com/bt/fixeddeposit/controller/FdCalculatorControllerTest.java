package com.bt.fixeddeposit.controller;

import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.exception.CalculationNotFoundException;
import com.bt.fixeddeposit.exception.CustomerNotFoundException;
import com.bt.fixeddeposit.exception.InvalidCalculationDataException;
import com.bt.fixeddeposit.service.FdCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FdCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FdCalculationService calculationService;

    private FdCalculationRequest validRequest;
    private FdCalculationResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = FdCalculationRequest.builder()
                .customerId(1L)
                .productCode("FD-001")
                .principalAmount(BigDecimal.valueOf(100000))
                .tenureMonths(12)
                .compoundingFrequency(4)
                .build();

        validResponse = FdCalculationResponse.builder()
                .id(1L)
                .customerId(1L)
                .productCode("FD-001")
                .productName("Fixed Deposit - Regular")
                .principalAmount(BigDecimal.valueOf(100000))
                .tenureMonths(12)
                .interestRate(BigDecimal.valueOf(6.5))
                .compoundingFrequency(4)
                .maturityAmount(BigDecimal.valueOf(106659.46))
                .interestEarned(BigDecimal.valueOf(6659.46))
                .effectiveRate(BigDecimal.valueOf(6.66))
                .currency("USD")
                .calculationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCalculateFd_Success() throws Exception {
        when(calculationService.calculateFd(any(FdCalculationRequest.class), anyString()))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/fd/calculate")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.principalAmount").value(100000))
                .andExpect(jsonPath("$.data.tenureMonths").value(12));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCalculateFd_InvalidRequest() throws Exception {
        FdCalculationRequest invalidRequest = FdCalculationRequest.builder()
                .customerId(1L)
                .productCode("")
                .principalAmount(BigDecimal.valueOf(100))
                .tenureMonths(0)
                .build();

        mockMvc.perform(post("/api/fd/calculate")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCalculateFd_CustomerNotFound() throws Exception {
        when(calculationService.calculateFd(any(FdCalculationRequest.class), anyString()))
                .thenThrow(new CustomerNotFoundException("Customer not found"));

        mockMvc.perform(post("/api/fd/calculate")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Customer Not Found"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCalculateFd_InvalidCalculationData() throws Exception {
        when(calculationService.calculateFd(any(FdCalculationRequest.class), anyString()))
                .thenThrow(new InvalidCalculationDataException("Invalid principal amount"));

        mockMvc.perform(post("/api/fd/calculate")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Calculation Data"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCalculationById_Success() throws Exception {
        when(calculationService.getCalculationById(eq(1L), anyString()))
                .thenReturn(validResponse);

        mockMvc.perform(get("/api/fd/calculations/1")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productCode").value("FD-001"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCalculationById_NotFound() throws Exception {
        when(calculationService.getCalculationById(eq(999L), anyString()))
                .thenThrow(new CalculationNotFoundException("Calculation not found"));

        mockMvc.perform(get("/api/fd/calculations/999")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Calculation Not Found"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetCalculationHistory_Success() throws Exception {
        List<FdCalculationResponse> history = Arrays.asList(validResponse, validResponse);
        when(calculationService.getCalculationHistory(eq(1L), anyString()))
                .thenReturn(history);

        mockMvc.perform(get("/api/fd/history/1")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "BANKOFFICER")
    void testGetRecentCalculations_Success() throws Exception {
        List<FdCalculationResponse> recentCalculations = Arrays.asList(validResponse);
        when(calculationService.getRecentCalculations(eq(1L), eq(30), anyString()))
                .thenReturn(recentCalculations);

        mockMvc.perform(get("/api/fd/recent/1")
                .param("days", "30")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testCalculateFd_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/fd/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCalculationHistory_AdminAccess() throws Exception {
        List<FdCalculationResponse> history = Arrays.asList(validResponse);
        when(calculationService.getCalculationHistory(eq(1L), anyString()))
                .thenReturn(history);

        mockMvc.perform(get("/api/fd/history/1")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
