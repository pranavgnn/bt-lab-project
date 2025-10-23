package com.bt.accounts.service;

import com.bt.accounts.client.*;
import com.bt.accounts.dto.*;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.*;
import com.bt.accounts.repository.FdAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock
    private FdAccountRepository accountRepository;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private FdCalculatorServiceClient fdCalculatorServiceClient;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private AccountService accountService;

    private AccountCreationRequest validRequest;
    private CustomerDto customerDto;
    private ProductDto productDto;
    private FdCalculationDto calculationDto;
    private String authToken;

    @BeforeEach
    void setUp() {
        authToken = "Bearer test-token";

        validRequest = AccountCreationRequest.builder()
                .customerId("CUST001")
                .productCode("FD-PREMIUM")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("6.75"))
                .tenureMonths(24)
                .branchCode("BR001")
                .build();

        customerDto = CustomerDto.builder()
                .customerId("CUST001")
                .firstName("John")
                .lastName("Doe")
                .accountStatus("ACTIVE")
                .build();

        productDto = ProductDto.builder()
                .productCode("FD-PREMIUM")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("10000000"))
                .minTermMonths(6)
                .maxTermMonths(120)
                .minInterestRate(new BigDecimal("5.00"))
                .maxInterestRate(new BigDecimal("8.00"))
                .status("ACTIVE")
                .build();

        calculationDto = FdCalculationDto.builder()
                .maturityAmount(new BigDecimal("114061.37"))
                .interestEarned(new BigDecimal("14061.37"))
                .effectiveRate(new BigDecimal("6.96"))
                .build();

        setupSecurityContext();
    }

    private void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_BANKOFFICER"));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createAccount_WithValidData_ShouldSucceed() {
        when(customerServiceClient.getCustomerById(any(), any()))
                .thenReturn(ApiResponse.<CustomerDto>builder().data(customerDto).build());
        when(productServiceClient.getProductByCode(any(), any()))
                .thenReturn(ApiResponse.<ProductDto>builder().data(productDto).build());
        when(fdCalculatorServiceClient.calculateFd(any(), any()))
                .thenReturn(ApiResponse.<FdCalculationDto>builder().data(calculationDto).build());
        when(accountNumberGenerator.generateAccountNumber(any()))
                .thenReturn("FD-BR001-20251023-10000001");
        when(accountRepository.save(any(FdAccount.class)))
                .thenAnswer(invocation -> {
                    FdAccount account = invocation.getArgument(0);
                    account.setId(1L);
                    return account;
                });

        AccountResponse response = accountService.createAccount(validRequest, authToken);

        assertNotNull(response);
        assertEquals("CUST001", response.getCustomerId());
        assertEquals("FD-PREMIUM", response.getProductCode());
        assertEquals(new BigDecimal("100000.00"), response.getPrincipalAmount());
        verify(accountRepository, times(1)).save(any(FdAccount.class));
    }

    @Test
    void createAccount_WithInvalidCustomer_ShouldThrowException() {
        when(customerServiceClient.getCustomerById(any(), any()))
                .thenReturn(ApiResponse.<CustomerDto>builder().data(null).build());

        assertThrows(CustomerNotFoundException.class, () -> accountService.createAccount(validRequest, authToken));
    }

    @Test
    void createAccount_WithPrincipalBelowMinimum_ShouldThrowException() {
        validRequest.setPrincipalAmount(new BigDecimal("5000"));

        when(customerServiceClient.getCustomerById(any(), any()))
                .thenReturn(ApiResponse.<CustomerDto>builder().data(customerDto).build());
        when(productServiceClient.getProductByCode(any(), any()))
                .thenReturn(ApiResponse.<ProductDto>builder().data(productDto).build());

        assertThrows(InvalidAccountDataException.class, () -> accountService.createAccount(validRequest, authToken));
    }

    @Test
    void createAccount_WithTenureAboveMaximum_ShouldThrowException() {
        validRequest.setTenureMonths(150);

        when(customerServiceClient.getCustomerById(any(), any()))
                .thenReturn(ApiResponse.<CustomerDto>builder().data(customerDto).build());
        when(productServiceClient.getProductByCode(any(), any()))
                .thenReturn(ApiResponse.<ProductDto>builder().data(productDto).build());

        assertThrows(InvalidAccountDataException.class, () -> accountService.createAccount(validRequest, authToken));
    }

    @Test
    void getAccount_WithValidAccountNo_ShouldReturnAccount() {
        FdAccount account = FdAccount.builder()
                .id(1L)
                .accountNo("FD-BR001-20251023-10000001")
                .customerId("CUST001")
                .status(FdAccount.AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNo("FD-BR001-20251023-10000001"))
                .thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount("FD-BR001-20251023-10000001");

        assertNotNull(response);
        assertEquals("FD-BR001-20251023-10000001", response.getAccountNo());
        assertEquals("CUST001", response.getCustomerId());
    }

    @Test
    void getAccount_WithInvalidAccountNo_ShouldThrowException() {
        when(accountRepository.findByAccountNo("INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount("INVALID"));
    }

    @Test
    void closeAccount_WithActiveAccount_ShouldSucceed() {
        FdAccount account = FdAccount.builder()
                .id(1L)
                .accountNo("FD-BR001-20251023-10000001")
                .status(FdAccount.AccountStatus.ACTIVE)
                .build();

        AccountClosureRequest request = AccountClosureRequest.builder()
                .closureReason("Customer requested premature closure")
                .build();

        when(accountRepository.findByAccountNo("FD-BR001-20251023-10000001"))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(FdAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.closeAccount("FD-BR001-20251023-10000001", request);

        assertNotNull(response);
        assertEquals("CLOSED", response.getStatus());
        assertNotNull(response.getClosedAt());
        verify(accountRepository, times(1)).save(any(FdAccount.class));
    }

    @Test
    void closeAccount_WithAlreadyClosedAccount_ShouldThrowException() {
        FdAccount account = FdAccount.builder()
                .id(1L)
                .accountNo("FD-BR001-20251023-10000001")
                .status(FdAccount.AccountStatus.CLOSED)
                .build();

        AccountClosureRequest request = AccountClosureRequest.builder()
                .closureReason("Test")
                .build();

        when(accountRepository.findByAccountNo("FD-BR001-20251023-10000001"))
                .thenReturn(Optional.of(account));

        assertThrows(AccountAlreadyClosedException.class,
                () -> accountService.closeAccount("FD-BR001-20251023-10000001", request));
    }

    @Test
    void getCustomerAccounts_WithValidCustomerId_ShouldReturnAccounts() {
        List<FdAccount> accounts = Arrays.asList(
                FdAccount.builder().id(1L).accountNo("ACC001").customerId("CUST001").build(),
                FdAccount.builder().id(2L).accountNo("ACC002").customerId("CUST001").build());

        when(accountRepository.findAllByCustomerIdOrderByCreatedAtDesc("CUST001"))
                .thenReturn(accounts);

        List<AccountResponse> responses = accountService.getCustomerAccounts("CUST001");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("ACC001", responses.get(0).getAccountNo());
        assertEquals("ACC002", responses.get(1).getAccountNo());
    }
}
