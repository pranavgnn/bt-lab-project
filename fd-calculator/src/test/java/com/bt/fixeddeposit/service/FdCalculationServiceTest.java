package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.client.CustomerServiceClient;
import com.bt.fixeddeposit.client.ProductServiceClient;
import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.dto.external.CustomerResponse;
import com.bt.fixeddeposit.dto.external.ExternalApiResponse;
import com.bt.fixeddeposit.dto.external.ProductResponse;
import com.bt.fixeddeposit.entity.FdCalculation;
import com.bt.fixeddeposit.exception.*;
import com.bt.fixeddeposit.repository.FdCalculationRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FdCalculationServiceTest {

    @Mock
    private FdCalculationRepository calculationRepository;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private FdCalculationService calculationService;

    private FdCalculationRequest validRequest;
    private ProductResponse validProduct;
    private CustomerResponse validCustomer;
    private FdCalculation savedCalculation;
    private String authToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(calculationService, "defaultCompoundingFrequency", 4);
        ReflectionTestUtils.setField(calculationService, "roundingScale", 2);

        authToken = "Bearer valid-token";

        validRequest = FdCalculationRequest.builder()
                .customerId(1L)
                .productCode("FD-001")
                .principalAmount(BigDecimal.valueOf(100000))
                .tenureMonths(12)
                .compoundingFrequency(4)
                .build();

        validCustomer = CustomerResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .fullName("Test User")
                .role("CUSTOMER")
                .active(true)
                .build();

        validProduct = ProductResponse.builder()
                .id(1L)
                .productCode("FD-001")
                .productName("Fixed Deposit - Regular")
                .productType("FIXED_DEPOSIT")
                .minInterestRate(BigDecimal.valueOf(6.5))
                .maxInterestRate(BigDecimal.valueOf(7.5))
                .minTermMonths(6)
                .maxTermMonths(120)
                .minAmount(BigDecimal.valueOf(10000))
                .maxAmount(BigDecimal.valueOf(10000000))
                .currency("USD")
                .status("ACTIVE")
                .build();

        savedCalculation = FdCalculation.builder()
                .id(1L)
                .customerId(1L)
                .productCode("FD-001")
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
    void testCalculateFd_Success() {
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);
        when(calculationRepository.save(any(FdCalculation.class))).thenReturn(savedCalculation);

        FdCalculationResponse response = calculationService.calculateFd(validRequest, authToken);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.valueOf(100000), response.getPrincipalAmount());
        assertEquals(12, response.getTenureMonths());
        assertNotNull(response.getMaturityAmount());
        assertTrue(response.getMaturityAmount().compareTo(response.getPrincipalAmount()) > 0);

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(productServiceClient).getProductByCode(eq("FD-001"), eq(authToken));
        verify(calculationRepository).save(any(FdCalculation.class));
    }

    @Test
    void testCalculateFd_CustomerNotFound() {
        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken)))
                .thenThrow(mock(FeignException.class));

        assertThrows(ServiceIntegrationException.class, () -> calculationService.calculateFd(validRequest, authToken));

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(productServiceClient, never()).getProductByCode(anyString(), anyString());
        verify(calculationRepository, never()).save(any());
    }

    @Test
    void testCalculateFd_ProductNotFound() {
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken)))
                .thenThrow(mock(FeignException.class));

        assertThrows(ServiceIntegrationException.class, () -> calculationService.calculateFd(validRequest, authToken));

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(productServiceClient).getProductByCode(eq("FD-001"), eq(authToken));
        verify(calculationRepository, never()).save(any());
    }

    @Test
    void testCalculateFd_InactiveCustomer() {
        validCustomer.setActive(false);
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);

        assertThrows(InvalidCalculationDataException.class,
                () -> calculationService.calculateFd(validRequest, authToken));

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(productServiceClient, never()).getProductByCode(anyString(), anyString());
    }

    @Test
    void testCalculateFd_InactiveProduct() {
        validProduct.setStatus("INACTIVE");
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        assertThrows(InvalidCalculationDataException.class,
                () -> calculationService.calculateFd(validRequest, authToken));

        verify(productServiceClient).getProductByCode(eq("FD-001"), eq(authToken));
        verify(calculationRepository, never()).save(any());
    }

    @Test
    void testCalculateFd_InvalidPrincipalAmount() {
        validRequest.setPrincipalAmount(BigDecimal.valueOf(5000));
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        assertThrows(InvalidCalculationDataException.class,
                () -> calculationService.calculateFd(validRequest, authToken));

        verify(calculationRepository, never()).save(any());
    }

    @Test
    void testCalculateFd_InvalidTenure() {
        validRequest.setTenureMonths(3);
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        assertThrows(InvalidCalculationDataException.class,
                () -> calculationService.calculateFd(validRequest, authToken));

        verify(calculationRepository, never()).save(any());
    }

    @Test
    void testGetCalculationById_Success() {
        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        when(calculationRepository.findById(eq(1L))).thenReturn(Optional.of(savedCalculation));
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        FdCalculationResponse response = calculationService.getCalculationById(1L, authToken);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("FD-001", response.getProductCode());

        verify(calculationRepository).findById(eq(1L));
        verify(productServiceClient).getProductByCode(eq("FD-001"), eq(authToken));
    }

    @Test
    void testGetCalculationById_NotFound() {
        when(calculationRepository.findById(eq(1L))).thenReturn(Optional.empty());

        assertThrows(CalculationNotFoundException.class, () -> calculationService.getCalculationById(1L, authToken));

        verify(calculationRepository).findById(eq(1L));
        verify(productServiceClient, never()).getProductByCode(anyString(), anyString());
    }

    @Test
    void testGetCalculationHistory_Success() {
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        List<FdCalculation> calculations = Arrays.asList(savedCalculation, savedCalculation);

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(calculationRepository.findByCustomerIdOrderByCreatedAtDesc(eq(1L))).thenReturn(calculations);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        List<FdCalculationResponse> response = calculationService.getCalculationHistory(1L, authToken);

        assertNotNull(response);
        assertEquals(2, response.size());

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(calculationRepository).findByCustomerIdOrderByCreatedAtDesc(eq(1L));
    }

    @Test
    void testGetRecentCalculations_Success() {
        ExternalApiResponse<CustomerResponse> customerApiResponse = ExternalApiResponse.<CustomerResponse>builder()
                .success(true)
                .data(validCustomer)
                .build();

        ExternalApiResponse<ProductResponse> productApiResponse = ExternalApiResponse.<ProductResponse>builder()
                .success(true)
                .data(validProduct)
                .build();

        List<FdCalculation> calculations = Arrays.asList(savedCalculation);

        when(customerServiceClient.getCustomerById(eq(1L), eq(authToken))).thenReturn(customerApiResponse);
        when(calculationRepository.findRecentCalculationsByCustomer(eq(1L), any(LocalDateTime.class)))
                .thenReturn(calculations);
        when(productServiceClient.getProductByCode(eq("FD-001"), eq(authToken))).thenReturn(productApiResponse);

        List<FdCalculationResponse> response = calculationService.getRecentCalculations(1L, 30, authToken);

        assertNotNull(response);
        assertEquals(1, response.size());

        verify(customerServiceClient).getCustomerById(eq(1L), eq(authToken));
        verify(calculationRepository).findRecentCalculationsByCustomer(eq(1L), any(LocalDateTime.class));
    }
}
