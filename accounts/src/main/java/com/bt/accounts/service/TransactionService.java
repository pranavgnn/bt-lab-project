package com.bt.accounts.service;

import com.bt.accounts.dto.TransactionRequest;
import com.bt.accounts.dto.TransactionResponse;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.exception.InvalidAccountDataException;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.FdAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final FdAccountRepository accountRepository;

    @Transactional
    public TransactionResponse recordTransaction(String accountNo, TransactionRequest request) {
        FdAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));

        if (account.getStatus() == FdAccount.AccountStatus.CLOSED) {
            throw new InvalidAccountDataException("Cannot record transaction on closed account: " + accountNo);
        }

        String transactionId = generateTransactionId(accountNo);

        BigDecimal currentBalance = calculateCurrentBalance(accountNo);
        BigDecimal newBalance = calculateNewBalance(currentBalance, request);

        AccountTransaction transaction = AccountTransaction.builder()
                .transactionId(transactionId)
                .accountNo(accountNo)
                .transactionType(AccountTransaction.TransactionType.valueOf(request.getTransactionType()))
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .referenceNo(request.getReferenceNo())
                .processedBy(getCurrentUsername())
                .remarks(request.getRemarks())
                .build();

        AccountTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Recorded transaction: {} for account: {}", transactionId, accountNo);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(String accountNo) {
        if (!accountRepository.existsByAccountNo(accountNo)) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }

        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoOrderByTransactionDateDesc(accountNo);

        return transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactionsByDateRange(
            String accountNo, LocalDateTime startDate, LocalDateTime endDate) {

        if (!accountRepository.existsByAccountNo(accountNo)) {
            throw new AccountNotFoundException("Account not found: " + accountNo);
        }

        List<AccountTransaction> transactions = transactionRepository
                .findByAccountNoAndDateRange(accountNo, startDate, endDate);

        return transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateCurrentBalance(String accountNo) {
        List<AccountTransaction> transactions = transactionRepository.findByAccountNo(accountNo);
        if (transactions.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo).orElseThrow();
            return account.getPrincipalAmount();
        }
        return transactions.get(transactions.size() - 1).getBalanceAfter();
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, TransactionRequest request) {
        AccountTransaction.TransactionType type = AccountTransaction.TransactionType
                .valueOf(request.getTransactionType());

        return switch (type) {
            case DEPOSIT, INTEREST_CREDIT -> currentBalance.add(request.getAmount());
            case WITHDRAWAL, PENALTY_DEBIT, PREMATURE_CLOSURE, MATURITY_PAYOUT ->
                currentBalance.subtract(request.getAmount());
            case REVERSAL -> currentBalance;
        };
    }

    private String generateTransactionId(String accountNo) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TXN-%s-%s-%s", accountNo, timestamp, uuid);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
