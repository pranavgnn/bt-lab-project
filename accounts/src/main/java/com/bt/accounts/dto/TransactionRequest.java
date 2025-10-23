package com.bt.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL|INTEREST_CREDIT|PREMATURE_CLOSURE|MATURITY_PAYOUT|PENALTY_DEBIT|REVERSAL", message = "Invalid transaction type")
    private String transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNo;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}
