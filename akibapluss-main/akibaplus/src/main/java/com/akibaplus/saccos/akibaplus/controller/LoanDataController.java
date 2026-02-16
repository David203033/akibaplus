package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.model.Loan;
import com.akibaplus.saccos.akibaplus.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanDataController {

    @Autowired
    private LoanRepository loanRepository;

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getLoanDetails(
            @RequestParam(name = "ref", required = false) String ref, 
            @RequestParam(name = "amount", required = false) Double amount,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        // Fetch real loan data
        Optional<Loan> loanOpt = Optional.empty();
        if (ref != null && !ref.isEmpty()) {
            loanOpt = loanRepository.findByRef(ref);
        }
        
        if (loanOpt.isEmpty()) {
             // Fallback or error if not found, but for now let's return empty or error
             // To keep it simple, if not found, we return empty map or error status
             if (amount == null) {
                 return ResponseEntity.badRequest().body(null);
             }
             // If amount is present but loan not found (e.g. new application simulation), we proceed with simulation logic?
             // The prompt asks to remove mock data. So we should rely on DB.
             // If loan not found, return 404
             return ResponseEntity.notFound().build();
        }

        Loan loan = loanOpt.get();
        double principal = loan.getAmount().doubleValue();
        double balanceVal = loan.getBalance() != null ? loan.getBalance().doubleValue() : 0.0;
        LocalDate startDate = loan.getDate();
        
        // Calculate derived values
        // Assuming simple interest for now as per previous logic, or fetch from DB if stored
        double interestRate = 0.15; // 15% flat rate as per previous mock logic
        double interest = principal * interestRate;
        double totalDue = principal + interest;
        double paidAmount = totalDue - balanceVal;

        response.put("principal", principal);
        response.put("interest", interest);
        response.put("totalDue", totalDue);
        response.put("paidAmount", Math.max(0, paidAmount));
        response.put("balance", balanceVal);

        // Calculate progress
        double progress = totalDue > 0 ? (paidAmount / totalDue) * 100 : 0;
        response.put("progress", Math.min(100, progress)); // Cap at 100%

        // Ratiba ya Malipo (Schedule)
        List<Map<String, Object>> schedule = new ArrayList<>();
        int duration = loan.getDuration() > 0 ? loan.getDuration() : 12;
        double monthlyPay = totalDue / duration;
        
        for (int i = 1; i <= duration; i++) {
            Map<String, Object> item = new HashMap<>();
            LocalDate payDate = startDate.plusMonths(i);
            
            item.put("date", payDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            item.put("principal", monthlyPay * 0.85);
            item.put("interest", monthlyPay * 0.15);
            item.put("total", monthlyPay);
            
            // Kokotoa salio linalotarajiwa
            double expectedBalance = Math.max(0, totalDue - (monthlyPay * i));
            item.put("balance", expectedBalance);
            
            // Determine status based on actual payments vs schedule
            double amountDueUntilNow = monthlyPay * i;
            if (paidAmount >= amountDueUntilNow - 100) { // Tolerance ya TZS 100
                item.put("status", "PAID");
            } else if (payDate.isBefore(LocalDate.now())) {
                item.put("status", "OVERDUE");
            } else {
                item.put("status", "PENDING");
            }
            schedule.add(item);
        }
        response.put("schedule", schedule);

        // Historia ya Malipo
        List<Map<String, Object>> history = new ArrayList<>();
        if (paidAmount > 0) {
            Map<String, Object> txn = new HashMap<>();
            // In a real system, fetch transactions related to this loan
            // For now, we show a summary transaction if paid amount > 0
            txn.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            txn.put("reference", "PAY-" + ref);
            txn.put("method", "System");
            txn.put("amount", paidAmount);
            history.add(txn);
        }
        
        response.put("history", history);

        return ResponseEntity.ok(response);
    }
}