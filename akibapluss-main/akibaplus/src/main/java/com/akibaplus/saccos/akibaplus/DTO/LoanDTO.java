package com.akibaplus.saccos.akibaplus.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanDTO {
    private Long id;
    private String ref;
    private String type;
    private BigDecimal amount;
    private BigDecimal balance;
    private BigDecimal nextPayment;
    private LocalDate date;
    private LocalDate dueDate;
    private String status;
    private int progress;

    // Constructor
    public LoanDTO(Long id, String ref, String type, BigDecimal amount, BigDecimal balance, BigDecimal nextPayment, LocalDate date, LocalDate dueDate, String status, int progress) {
        this.id = id;
        this.ref = ref;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.nextPayment = nextPayment;
        this.date = date;
        this.dueDate = dueDate;
        this.status = status;
        this.progress = progress;
    }

    // Getters
    public Long getId() { return id; }
    public String getRef() { return ref; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getNextPayment() { return nextPayment; }
    public LocalDate getDate() { return date; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
}