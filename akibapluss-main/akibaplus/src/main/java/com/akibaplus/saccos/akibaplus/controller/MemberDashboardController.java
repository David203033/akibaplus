package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.DTO.MemberProfileDTO;
import com.akibaplus.saccos.akibaplus.DTO.LoanDTO;
import com.akibaplus.saccos.akibaplus.DTO.LoanEligibilityDTO;
import com.akibaplus.saccos.akibaplus.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class MemberDashboardController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@user.com";

        // 1. Fetch Core Data
        MemberProfileDTO profile = memberService.getMemberProfile(email);
        BigDecimal savingsBalance = memberService.getSavingsBalance(email);
        BigDecimal shareValue = memberService.getShareValue(email);
        BigDecimal loanBalance = memberService.getLoanBalance(email);
        List<LoanDTO> loans = memberService.getLoans(email);

        // 2. Populate Dashboard Tab
        model.addAttribute("memberName", profile.getFirstName());
        model.addAttribute("savingsBalance", savingsBalance);
        model.addAttribute("shareValue", shareValue);
        model.addAttribute("loanBalance", loanBalance);
        model.addAttribute("memberStatus", profile.getStatus());
        model.addAttribute("transactions", memberService.getRecentTransactions(email));
        model.addAttribute("membershipNumber", profile.getMembershipNumber());
        
        List<Map<String, Object>> notifications = memberService.getNotifications(email);
        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationCount", notifications.size());
        model.addAttribute("profileImageUrl", profile.getProfileImageUrl());

        // 3. Populate Savings Tab
        model.addAttribute("interestEarned", memberService.getInterestEarned(email));
        model.addAttribute("depositCount", memberService.getDepositCount(email));
        model.addAttribute("joinDate", profile.getJoinedOn().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        // Note: 'transactions' attribute is reused here as per member.html logic

        // 4. Populate Shares Tab
        model.addAttribute("shareCount", memberService.getShareCount(email));
        model.addAttribute("dividends", memberService.getDividends(email));
        model.addAttribute("ownershipPercentage", memberService.getOwnershipPercentage(email));
        model.addAttribute("shareTransactions", memberService.getShareTransactions(email));
        model.addAttribute("marketShares", new java.util.ArrayList<>()); // Placeholder for future market data

        // 5. Populate Loans Tab
        model.addAttribute("loans", loans);
        // Calculate next payment from active loans
        BigDecimal nextPayment = loans.stream()
                .map(LoanDTO::getNextPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("nextPaymentAmount", nextPayment);
        model.addAttribute("loanLimit", memberService.getLoanLimit(email));
        LoanEligibilityDTO eligibility = memberService.checkLoanEligibility(email);
        model.addAttribute("loanEligibility", eligibility);
        model.addAttribute("creditScore", memberService.getCreditScore(email));

        // 6. Populate Loan Apply Tab
        model.addAttribute("membershipDuration", "3 miaka"); // Logic can be added to calculate from joinedOn

        // 7. Populate Guarantor Tab
        List<Map<String, Object>> requests = memberService.getGuarantorRequests(email);
        model.addAttribute("guarantorRequests", requests);
        model.addAttribute("pendingGuarantorRequests", requests.size());
        model.addAttribute("acceptedGuarantees", 0);
        model.addAttribute("totalGuaranteedAmount", BigDecimal.ZERO);
        model.addAttribute("activeGuarantees", memberService.getActiveGuarantees(email));

        // 8. Populate Meetings Tab
        model.addAttribute("upcomingMeetings", memberService.getUpcomingMeetings());
        model.addAttribute("pastMeetings", memberService.getPastMeetings());

        // 9. Populate Penalties Tab
        List<Map<String, Object>> allPenalties = memberService.getPenalties(email);
        List<Map<String, Object>> unpaid = allPenalties.stream().filter(p -> !"PAID".equals(p.get("status"))).toList();
        List<Map<String, Object>> paid = allPenalties.stream().filter(p -> "PAID".equals(p.get("status"))).toList();
        model.addAttribute("penalties", unpaid);
        model.addAttribute("paidPenalties", paid);
        // compute penalty totals
        BigDecimal unpaidTotal = allPenalties.stream()
                .filter(p -> !"PAID".equals(p.get("status")))
                .map(p -> p.get("amount") instanceof BigDecimal ? (BigDecimal)p.get("amount") : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidTotal = allPenalties.stream()
                .filter(p -> "PAID".equals(p.get("status")))
                .map(p -> p.get("amount") instanceof BigDecimal ? (BigDecimal)p.get("amount") : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("unpaidPenaltiesTotal", unpaidTotal);
        model.addAttribute("paidPenaltiesTotal", paidTotal);
        model.addAttribute("totalPenalties", unpaidTotal.add(paidTotal));
        model.addAttribute("actionablePenalties", allPenalties.size());

        // 11. Statements - create a simple recent statement when none exist
        List<Map<String, Object>> statements = new ArrayList<>();
        List<Map<String, Object>> recentTx = memberService.getRecentTransactions(email);
        if (!recentTx.isEmpty()) {
            Map<String, Object> stmt = new java.util.HashMap<>();
            stmt.put("period", "Last 30 days");
            stmt.put("type", "Akaunti Zote");
            stmt.put("summary", recentTx.size() + " shughuli");
            stmt.put("date", java.time.LocalDate.now().toString());
            statements.add(stmt);
        }
        model.addAttribute("statements", statements);

        // 12. Populate additional analytics-like attributes for template
        model.addAttribute("totalDeposits", memberService.getTotalDeposits(email));
        model.addAttribute("totalCharges", memberService.getTotalCharges(email));
        model.addAttribute("netBalance", savingsBalance.subtract(memberService.getTotalCharges(email)));
        model.addAttribute("totalTransactionsCount", memberService.getTotalTransactionsCount(email));
        model.addAttribute("interestReceived", memberService.getInterestEarned(email));
        model.addAttribute("expectedDividend", memberService.getDividends(email));
        model.addAttribute("savingsGrowth", "0.0%");
        model.addAttribute("memberScore", memberService.getCreditScore(email));

        // 10. Populate Profile Tab
        model.addAttribute("profile", profile);

        return "member";
    }

    @GetMapping(path = "/analytics-data", produces = "application/json")
    public @org.springframework.web.bind.annotation.ResponseBody java.util.Map<String, Object> analyticsData(Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@user.com";
        return memberService.getAnalyticsSummary(email);
    }
}