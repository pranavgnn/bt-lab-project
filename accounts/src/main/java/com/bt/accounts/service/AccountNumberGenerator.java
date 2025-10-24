package com.bt.accounts.service;

import com.bt.accounts.repository.FdAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final FdAccountRepository accountRepository;
    private final AtomicLong sequenceCounter = new AtomicLong(10000001);

    public synchronized String generateAccountNumber(String branchCode) {
        String dateComponent = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Long todayCount = accountRepository.countTodayAccountsByBranch(branchCode);
        long sequence = sequenceCounter.incrementAndGet() + todayCount;

        return String.format("FD-%s-%s-%08d", branchCode, dateComponent, sequence);
    }
}
