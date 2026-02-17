package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.model.Loan;
import com.akibaplus.saccos.akibaplus.model.Meeting;
import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.Notification;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import com.akibaplus.saccos.akibaplus.model.User;
import com.akibaplus.saccos.akibaplus.repository.MeetingRepository;
import com.akibaplus.saccos.akibaplus.repository.LoanRepository;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import com.akibaplus.saccos.akibaplus.repository.NotificationRepository;
import com.akibaplus.saccos.akibaplus.repository.TransactionRepository;
import com.akibaplus.saccos.akibaplus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String getAdminDashboard(Model model, Authentication authentication) {
        // Fetch all transactions once for calculations
        if (authentication != null) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(u -> {
                model.addAttribute("adminName", u.getName());
            });
        }

        List<Transaction> allTransactions = transactionRepository.findAll();

        // --- 1. Dashboard Stats ---
        Map<String, Object> dashboardStats = new HashMap<>();
        List<Member> allMembers = memberRepository.findAll();
        long totalMembers = allMembers.size();
        dashboardStats.put("totalMembers", totalMembers);
        
        long newMembersThisMonth = allMembers.stream()
                .filter(m -> m.getJoinedOn() != null && m.getJoinedOn().getMonth() == LocalDate.now().getMonth() && m.getJoinedOn().getYear() == LocalDate.now().getYear())
                .count();
        dashboardStats.put("newMembersThisMonth", newMembersThisMonth);

        List<Map<String, Object>> membersList = allMembers.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", m.getFullName());
            map.put("id", m.getMembershipNumber());
            map.put("phone", m.getPhone());
            map.put("joinDate", m.getJoinedOn());
            map.put("status", m.getStatus());
            map.put("balance", m.getSavingsBalance() != null ? m.getSavingsBalance() : BigDecimal.ZERO);
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("membersList", membersList);

        // Savings
        BigDecimal totalSavings = allMembers.stream()
                .map(m -> m.getSavingsBalance() != null ? m.getSavingsBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboardStats.put("totalSavings", totalSavings);
        
        // Savings Growth
        LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        BigDecimal netSavingsChangeThisYear = allTransactions.stream()
                .filter(t -> t.getDate() != null && !t.getDate().isBefore(startOfYear))
                .filter(t -> "AMANA".equalsIgnoreCase(t.getType()) || "TOZO".equalsIgnoreCase(t.getType()) || "WITHDRAWAL".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal savingsLastYear = totalSavings.subtract(netSavingsChangeThisYear);
        double savingsGrowthVal = 0.0;
        if (savingsLastYear.compareTo(BigDecimal.ZERO) > 0) {
            savingsGrowthVal = netSavingsChangeThisYear.divide(savingsLastYear, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        } else if (totalSavings.compareTo(BigDecimal.ZERO) > 0) {
            savingsGrowthVal = 100.0;
        }
        dashboardStats.put("savingsGrowthPercentage", (int) savingsGrowthVal);

        List<Map<String, Object>> savingsList = allMembers.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("member", m.getFullName());
            BigDecimal balance = m.getSavingsBalance() != null ? m.getSavingsBalance() : BigDecimal.ZERO;
            map.put("balance", balance);
            map.put("interest", balance.multiply(new BigDecimal("0.05"))); // Mock 5% interest
            map.put("status", m.getStatus());
            map.put("since", m.getJoinedOn() != null ? String.valueOf(m.getJoinedOn().getYear()) : "-");
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("savingsList", savingsList);

        // Loans
        List<Loan> allLoans = loanRepository.findAll();
        List<Loan> activeLoans = allLoans.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()) || "INAYOTUMIKA".equalsIgnoreCase(l.getStatus())
                        || "OVERDUE".equalsIgnoreCase(l.getStatus()))
                .collect(Collectors.toList());
        
        BigDecimal activeLoansAmount = activeLoans.stream()
                .map(l -> l.getBalance() != null ? l.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboardStats.put("activeLoansAmount", activeLoansAmount);
        dashboardStats.put("activeLoansCount", activeLoans.size());
        
        BigDecimal totalLoanPortfolio = allLoans.stream()
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingLoansCount = allLoans.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())).count();
        long pendingMembersCount = allMembers.stream().filter(m -> "PENDING".equalsIgnoreCase(m.getStatus())).count();
        dashboardStats.put("pendingRequestsCount", pendingLoansCount + pendingMembersCount);
        dashboardStats.put("pendingLoansCount", pendingLoansCount);
        dashboardStats.put("pendingMembersCount", pendingMembersCount);

        // NPL & Repayment
        BigDecimal nplAmount = activeLoans.stream()
                .filter(l -> {
                    if (l.getDate() == null) return false;
                    int duration = l.getDuration() > 0 ? l.getDuration() : 1;
                    LocalDate dueDate = l.getDate().plusMonths(duration);
                    return LocalDate.now().isAfter(dueDate) && l.getBalance().compareTo(BigDecimal.ZERO) > 0;
                })
                .map(l -> l.getBalance() != null ? l.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double nplRatioVal = activeLoansAmount.compareTo(BigDecimal.ZERO) > 0 
                ? nplAmount.divide(activeLoansAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 
                : 0.0;

        double onTimeRateVal = 100.0 - nplRatioVal;
        double outstandingPct = totalLoanPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? activeLoansAmount.divide(totalLoanPortfolio, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        dashboardStats.put("totalOutstandingLoans", activeLoansAmount);
        dashboardStats.put("outstandingLoansPercentage", (int) outstandingPct);
        dashboardStats.put("nplRatio", (int) nplRatioVal);
        dashboardStats.put("onTimeRepaymentRate", (int) onTimeRateVal);
        model.addAttribute("dashboardStats", dashboardStats);

        List<Map<String, Object>> loansList = activeLoans.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("member", l.getMember().getFullName());
            map.put("amount", l.getAmount());
            map.put("type", l.getType());
            map.put("balance", l.getBalance());
            map.put("date", l.getDate());
            map.put("dueDate", l.getDate().plusMonths(l.getDuration()));
            map.put("status", l.getStatus());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("loansList", loansList);

        // --- 2. Member Stats ---
        Map<String, Object> memberStats = new HashMap<>();
        memberStats.put("totalMembers", totalMembers);
        memberStats.put("newMembersThisMonth", newMembersThisMonth);
        long activeMembers = allMembers.stream().filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus())).count();
        memberStats.put("activeMembers", activeMembers);
        memberStats.put("activePercentage", totalMembers > 0 ? (activeMembers * 100 / totalMembers) : 0);
        long inactiveMembers = totalMembers - activeMembers - pendingMembersCount;
        memberStats.put("inactiveMembers", inactiveMembers);
        memberStats.put("inactivePercentage", totalMembers > 0 ? (inactiveMembers * 100 / totalMembers) : 0);
        memberStats.put("pendingMembers", pendingMembersCount);
        model.addAttribute("memberStats", memberStats);

        // --- 3. Loan Stats ---
        Map<String, Object> loanStats = new HashMap<>();
        loanStats.put("activeLoansCount", activeLoans.size());
        loanStats.put("totalLoanPortfolio", totalLoanPortfolio);
        loanStats.put("portfolioGrowth", 8.5);
        loanStats.put("outstandingAmount", activeLoansAmount);
        loanStats.put("outstandingPercentage", (int) outstandingPct);
        loanStats.put("nplRatio", (int) nplRatioVal);
        loanStats.put("pendingLoansCount", pendingLoansCount);
        long atRiskLoansCount = activeLoans.stream().filter(l -> "OVERDUE".equalsIgnoreCase(l.getStatus())).count();
        loanStats.put("atRiskLoansCount", atRiskLoansCount);
        model.addAttribute("loanStats", loanStats);

        // --- 4. Savings Stats ---
        Map<String, Object> savingsStats = new HashMap<>();
        savingsStats.put("totalSavings", totalSavings);
        savingsStats.put("growthPercentage", (int) savingsGrowthVal);
        long activeSavingsAccounts = allMembers.stream().filter(m -> m.getSavingsBalance().compareTo(BigDecimal.ZERO) > 0).count();
        savingsStats.put("activeAccounts", activeSavingsAccounts);
        savingsStats.put("activeAccountsPercentage", totalMembers > 0 ? (activeSavingsAccounts * 100 / totalMembers) : 0);
        BigDecimal totalInterestPaid = allTransactions.stream()
            .filter(t -> "INTEREST".equalsIgnoreCase(t.getType()) || "RIBA".equalsIgnoreCase(t.getType()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        savingsStats.put("totalInterestPaid", totalInterestPaid);
        savingsStats.put("pendingDepositsCount", 0);
        model.addAttribute("savingsStats", savingsStats);

        // --- 5. Share Stats ---
        Map<String, Object> shareStats = new HashMap<>();
        BigDecimal totalShares = allMembers.stream()
                .map(m -> m.getSharesValue() != null ? m.getSharesValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        shareStats.put("totalSharesValue", totalShares);
        long shareholdersCount = allMembers.stream()
                .filter(m -> m.getSharesValue() != null && m.getSharesValue().compareTo(BigDecimal.ZERO) > 0)
                .count();
        shareStats.put("shareholdersCount", shareholdersCount);
        BigDecimal sharePrice = new BigDecimal("10000"); // Mock price
        BigDecimal netProfit = totalShares.multiply(new BigDecimal("0.15")); // Mock: 15% of total shares value as profit
        BigDecimal dividendPool = netProfit.multiply(new BigDecimal("0.75")); // Mock: 75% of profit is for dividends
        shareStats.put("dividendPool", dividendPool);
        model.addAttribute("shareStats", shareStats);

        List<Map<String, Object>> shareholdersList = allMembers.stream()
                .filter(m -> m.getSharesValue() != null && m.getSharesValue().compareTo(BigDecimal.ZERO) > 0)
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("member", m.getFullName());
                    map.put("shares", m.getSharesValue().divideToIntegralValue(sharePrice));
                    map.put("shareValue", m.getSharesValue());
                    map.put("dividendEarned", m.getSharesValue().multiply(new BigDecimal("0.15")).multiply(new BigDecimal("0.75")));
                    map.put("date", LocalDate.now());
                    map.put("status", "ACTIVE");
                    return map;
                }).collect(Collectors.toList());
        model.addAttribute("shareholdersList", shareholdersList);

        // --- 8. User Stats ---
        Map<String, Object> userStats = new HashMap<>();
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> usersList = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRoles() != null ? u.getRoles().toString() : "USER");
            map.put("twoFactor", "Disabled");
            map.put("status", "Active");
            map.put("lastLogin", "Leo");
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("usersList", usersList);
        userStats.put("totalUsers", users.size());
        userStats.put("activeUsers", users.size()); // Assuming all active for now
        userStats.put("activePercentage", 100);
        userStats.put("newUsersThisMonth", 0);
        userStats.put("inactiveThisWeek", 0);
        model.addAttribute("userStats", userStats);

        // --- 6. Expense Stats ---
        Map<String, Object> expenseStats = new HashMap<>();
        List<Transaction> expenses = allTransactions.stream()
                .filter(t -> "MATUMIZI".equalsIgnoreCase(t.getType()) || "EXPENSE".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        BigDecimal totalExpensesThisYear = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear())
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpensesThisMonth = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear() && t.getDate().getMonth() == LocalDate.now().getMonth())
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        expenseStats.put("totalExpensesThisMonth", totalExpensesThisMonth);
        expenseStats.put("expenseLimit", new BigDecimal("5000000"));
        expenseStats.put("remainingBudget", new BigDecimal("5000000").subtract(totalExpensesThisMonth));
        expenseStats.put("pendingExpensesCount", 0);
        model.addAttribute("expenseStats", expenseStats);

        List<Map<String, Object>> expensesList = expenses.stream()
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", t.getDate());
                    map.put("desc", t.getDescription());
                    map.put("amount", t.getAmount());
                    map.put("status", "Imelipwa");
                    map.put("recordedBy", "Admin");
                    return map;
                }).collect(Collectors.toList());
        model.addAttribute("expensesList", expensesList);
        
        // Prepare Chart Data for Expenses
        Map<String, BigDecimal> expenseCategories = expenses.stream()
            .collect(Collectors.groupingBy(
                t -> {
                    String d = t.getDescription();
                    if(d != null && d.contains("[") && d.contains("]")) return d.substring(d.lastIndexOf("[")+1, d.lastIndexOf("]"));
                    return "Mengineyo";
                },
                Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add)
            ));
        model.addAttribute("expenseChartLabels", expenseCategories.keySet());
        model.addAttribute("expenseChartData", expenseCategories.values());

        // --- 7. Meeting Stats ---
        Map<String, Object> meetingStats = new HashMap<>();
        List<Meeting> allMeetings = meetingRepository.findAll();
        List<Map<String, Object>> meetingsList = allMeetings.stream()
                .sorted((m1, m2) -> m2.getDate().compareTo(m1.getDate())) // Sort by most recent first
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", m.getName());
                    String timeRange = m.getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                    if (m.getEndTime() != null) {
                        timeRange += " - " + m.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                    }
                    map.put("date", m.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + ", " + timeRange);
                    map.put("location", m.getLocation());
                    map.put("expected", m.getExpected());
                    map.put("attended", m.getAttended());
                    map.put("status", m.getStatus());
                    return map;
                }).collect(Collectors.toList());
        model.addAttribute("meetingsList", meetingsList);

        long upcomingMeetingsCount = allMeetings.stream()
                .filter(m -> m.getDate() != null && !m.getDate().isBefore(LocalDate.now()))
                .count();
        long pastMeetingsThisMonth = allMeetings.stream()
                .filter(m -> m.getDate() != null && m.getDate().isBefore(LocalDate.now()) && m.getDate().getMonth() == LocalDate.now().getMonth() && m.getDate().getYear() == LocalDate.now().getYear())
                .count();
        List<Meeting> pastMeetings = allMeetings.stream()
                .filter(m -> m.getDate() != null && m.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
        double totalExpected = pastMeetings.stream().mapToInt(m -> m.getExpected() > 0 ? m.getExpected() : (int)totalMembers).sum();
        double totalAttended = pastMeetings.stream().mapToInt(m -> m.getAttended()).sum();
        double avgAttendance = (totalExpected > 0) ? (totalAttended / totalExpected) * 100 : 0.0;

        meetingStats.put("upcomingMeetingsCount", upcomingMeetingsCount);
        meetingStats.put("pastMeetingsThisMonth", pastMeetingsThisMonth);
        meetingStats.put("averageAttendance", (int) avgAttendance);
        meetingStats.put("totalMembers", totalMembers);
        model.addAttribute("meetingStats", meetingStats);

        // --- 9. Fine Stats ---
        Map<String, Object> fineStats = new HashMap<>();
        List<Transaction> fines = allTransactions.stream().filter(t -> "FINE".equalsIgnoreCase(t.getType()) || "FAINI".equalsIgnoreCase(t.getType())).collect(Collectors.toList());
        BigDecimal totalFines = fines.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        fineStats.put("totalFines", totalFines);
        fineStats.put("totalFinesCount", fines.size());
        BigDecimal paidFines = fines.stream().filter(t -> "PAID".equalsIgnoreCase(t.getStatus()) || "COMPLETED".equalsIgnoreCase(t.getStatus())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        fineStats.put("paidFines", paidFines);
        fineStats.put("paidPercentage", totalFines.compareTo(BigDecimal.ZERO) > 0 ? paidFines.divide(totalFines, 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100)) : 0);
        model.addAttribute("fineStats", fineStats);
        
        List<Map<String, Object>> finesList = fines.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberName", t.getMember().getFullName());
            map.put("amount", t.getAmount());
            map.put("reason", t.getDescription());
            map.put("date", t.getDate());
            map.put("status", t.getStatus());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("finesList", finesList);
        
        // 9. Notifications Data (Real Data)
        List<Notification> allNotifications = notificationRepository.findAll(Sort.by(Sort.Direction.DESC, "sentAt"));
        List<Map<String, Object>> notificationsList = allNotifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("message", n.getMessage());
            map.put("type", n.getType());
            map.put("date", n.getSentAt() != null ? n.getSentAt().toLocalDate().toString() : "N/A");
            map.put("received", n.getRecipientCount());
            map.put("status", n.getStatus());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("notificationsList", notificationsList);

        // Map for Dashboard System Notifications (Top 5 recent)
        List<Map<String, Object>> systemNotifications = allNotifications.stream()
            .limit(5)
            .map(n -> {
                Map<String, Object> map = new HashMap<>();
                map.put("message", n.getMessage());
                String type = "info";
                String icon = "fas fa-info-circle";
                if (n.getMessage() != null && (n.getMessage().toLowerCase().contains("dharura") || n.getMessage().toLowerCase().contains("emergency"))) {
                    type = "danger";
                    icon = "fas fa-exclamation-circle";
                } else if (n.getMessage() != null && (n.getMessage().toLowerCase().contains("malipo") || n.getMessage().toLowerCase().contains("payment"))) {
                    type = "success";
                    icon = "fas fa-check-circle";
                }
                map.put("type", type);
                map.put("icon", icon);
                return map;
            }).collect(Collectors.toList());
        model.addAttribute("systemNotifications", systemNotifications);
        
        // Audit Logs (Mocking using notifications/transactions)
        List<Map<String, Object>> auditLogsList = allNotifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("user", "System");
            map.put("action", "Notification Sent");
            map.put("timestamp", n.getSentAt());
            map.put("ipAddress", "127.0.0.1");
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("auditLogsList", auditLogsList);

        // 8. Recent Activities (Real Data from Transactions)
        List<Transaction> recentTxns = transactionRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "date"))).getContent();
        List<Map<String, Object>> recentActivities = recentTxns.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            String type = t.getType() != null ? t.getType() : "UNKNOWN";
            map.put("title", type);
            map.put("desc", t.getDescription());
            map.put("time", t.getDate() != null ? t.getDate().toString() : "");
            String icon = "fas fa-circle";
            String color = "text-secondary";
            if ("AMANA".equalsIgnoreCase(type)) { icon = "fas fa-arrow-down"; color = "text-success"; }
            else if ("MKOPO".equalsIgnoreCase(type)) { icon = "fas fa-money-bill-wave"; color = "text-info"; }
            else if ("TOZO".equalsIgnoreCase(type)) { icon = "fas fa-arrow-up"; color = "text-danger"; }
            map.put("icon", icon);
            map.put("color", color);
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("recentActivities", recentActivities);

        // 10. Charts Data (Added for Reports & Dashboard)
        // Growth Chart (Last 5 Years Trend from Transactions)
        List<String> chartYears = new ArrayList<>();
        List<BigDecimal> chartSavingsTrend = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        
        for (int i = 4; i >= 0; i--) {
            int year = currentYear - i;
            chartYears.add(String.valueOf(year));
            
            final int y = year;
            BigDecimal savingsAtYear = allTransactions.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() <= y)
                .filter(t -> "AMANA".equalsIgnoreCase(t.getType()) || "DEPOSIT".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            // Subtract withdrawals if needed, assuming simple accumulation for now
            chartSavingsTrend.add(savingsAtYear);
        }
        model.addAttribute("chartYears", chartYears);
        model.addAttribute("chartSavingsTrend", chartSavingsTrend);
        
        // Portfolio Chart Data
        model.addAttribute("chartSavings", totalSavings);
        model.addAttribute("chartLoans", totalLoanPortfolio);
        model.addAttribute("chartShares", totalShares);

        // --- 10. Report Stats ---
        Map<String, Object> reportStats = new HashMap<>();
        reportStats.put("totalMembers", totalMembers);
        reportStats.put("newMembersThisMonth", newMembersThisMonth);
        reportStats.put("totalSavings", totalSavings);
        reportStats.put("savingsGrowthYoY", 15.0);
        reportStats.put("activeLoansValue", activeLoansAmount);
        reportStats.put("activeLoansCount", activeLoans.size());
        reportStats.put("totalSharesValue", totalShares);
        reportStats.put("shareholdersCount", shareholdersCount);
        reportStats.put("dividendPool", dividendPool);
        reportStats.put("nplRatio", nplRatioVal);
        model.addAttribute("reportStats", reportStats);

        // --- 11. Settings ---
        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultShareAmount", 50000);
        settings.put("expenseLimit", 5000000);
        settings.put("language", "sw");
        settings.put("timezone", "Africa/Dar_es_Salaam");
        model.addAttribute("settings", settings);
        
        // Financial Summary Table
        model.addAttribute("totalSavings", "TZS " + formatMoney(totalSavings));
        model.addAttribute("activeAccountsRate", "95%");
        model.addAttribute("totalLoanPortfolio", "TZS " + formatMoney(totalLoanPortfolio));
        model.addAttribute("activeLoansCount", activeLoans.size());
        model.addAttribute("totalShares", "TZS " + formatMoney(totalShares));
        model.addAttribute("shareholdersCount", shareholdersCount);
        model.addAttribute("dividendPool", dividendPool);
        model.addAttribute("dividendPercentage", "10%");

        return "admin";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    @PostMapping("/api/expenses/add")
    @ResponseBody
    public Map<String, Object> addExpense(@RequestBody Map<String, String> payload, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            Member saccosSystemMember = memberRepository.findByMembershipNumber("SACCOS_INTERNAL")
                    .orElseGet(() -> {
                        Member systemMember = new Member();
                        systemMember.setFirstName("AkibaPlus");
                        systemMember.setLastName("SACCOS");
                        systemMember.setMembershipNumber("SACCOS_INTERNAL");
                        systemMember.setStatus("SYSTEM");
                        return memberRepository.save(systemMember);
                    });

            Transaction t = new Transaction();
            t.setMember(saccosSystemMember);
            t.setDate(LocalDate.parse(payload.get("date")));
            t.setAmount(new BigDecimal(payload.get("amount")));
            t.setDescription(payload.get("description"));
            t.setType("EXPENSE");
            t.setStatus("COMPLETED");
            transactionRepository.save(t);
            
            response.put("success", true);
            response.put("message", "Matumizi yamehifadhiwa kikamilifu.");
            response.put("updatedData", getUpdatedExpensesData()); // Add updated data to the response
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    private Map<String, Object> getUpdatedExpensesData() {
        Map<String, Object> data = new HashMap<>();
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> expenses = allTransactions.stream()
                .filter(t -> "MATUMIZI".equalsIgnoreCase(t.getType()) || "EXPENSE".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        BigDecimal totalExpensesThisYear = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear())
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long expenseCountThisYear = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear())
                .count();

        BigDecimal totalExpensesThisMonth = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear() && t.getDate().getMonth() == LocalDate.now().getMonth())
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long expenseCountThisMonth = expenses.stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == LocalDate.now().getYear() && t.getDate().getMonth() == LocalDate.now().getMonth())
                .count();

        BigDecimal averageExpenseTransaction = expenseCountThisYear > 0 
                ? totalExpensesThisYear.divide(BigDecimal.valueOf(expenseCountThisYear), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        data.put("totalExpensesThisYear", totalExpensesThisYear);
        data.put("expenseCountThisYear", expenseCountThisYear);
        data.put("totalExpensesThisMonth", totalExpensesThisMonth);
        data.put("expenseCountThisMonth", expenseCountThisMonth);
        data.put("averageExpenseTransaction", averageExpenseTransaction);
        data.put("expensesRemainingBudget", "TZS " + formatMoney(new BigDecimal("10000000").subtract(totalExpensesThisYear)));
        
        long pendingExpensesCount = expenses.stream()
                .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()) || "WAITING".equalsIgnoreCase(t.getStatus()))
                .count();
        data.put("expensesPendingExpenses", pendingExpensesCount);
        data.put("paymentLimit", "TZS 50,000");

        List<Map<String, Object>> expensesList = expenses.stream()
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", t.getDate());
                    map.put("desc", t.getDescription());
                    map.put("amount", t.getAmount());
                    map.put("status", "Imelipwa");
                    map.put("recordedBy", "Admin");
                    return map;
                }).collect(Collectors.toList());
        data.put("expensesList", expensesList);
        
        Map<String, BigDecimal> expenseCategories = expenses.stream()
            .collect(Collectors.groupingBy(
                t -> {
                    String d = t.getDescription();
                    if(d != null && d.contains("[") && d.contains("]")) return d.substring(d.lastIndexOf("[")+1, d.lastIndexOf("]"));
                    return "Mengineyo";
                },
                Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add)
            ));
        data.put("expenseChartLabels", expenseCategories.keySet());
        data.put("expenseChartData", expenseCategories.values());

        // 7. Recent Activities (Update for real-time feed)
        List<Transaction> recentTxns = transactionRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "date"))).getContent();
        List<Map<String, Object>> recentActivities = recentTxns.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            String type = t.getType() != null ? t.getType() : "UNKNOWN";
            map.put("title", type);
            map.put("desc", t.getDescription());
            map.put("time", t.getDate() != null ? t.getDate().toString() : "");
            String icon = "fas fa-circle";
            String color = "text-secondary";
            if ("AMANA".equalsIgnoreCase(type)) { icon = "fas fa-arrow-down"; color = "text-success"; }
            else if ("MKOPO".equalsIgnoreCase(type)) { icon = "fas fa-money-bill-wave"; color = "text-info"; }
            else if ("TOZO".equalsIgnoreCase(type)) { icon = "fas fa-arrow-up"; color = "text-danger"; }
            map.put("icon", icon);
            map.put("color", color);
            return map;
        }).collect(Collectors.toList());
        data.put("recentActivities", recentActivities);

        return data;
    }

    @GetMapping("/api/meetings/list")
    @ResponseBody
    public List<Map<String, Object>> getMeetingsList() {
        List<Meeting> allMeetings = meetingRepository.findAll();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();

        return allMeetings.stream()
                .sorted((m1, m2) -> {
                    if (m1.getDate() == null || m2.getDate() == null) return 0;
                    return m2.getDate().compareTo(m1.getDate());
                })
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", m.getName());
                    String timeRange = m.getTime() != null ? m.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
                    if (m.getEndTime() != null) {
                        timeRange += " - " + m.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                    }
                    map.put("date", (m.getDate() != null ? m.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "") + ", " + timeRange);
                    map.put("location", m.getLocation());
                    // include counts (primitive fields)
                    map.put("expected", m.getExpected());
                    map.put("attended", m.getAttended());

                    // Compute dynamic status if meeting has ended
                    String status = m.getStatus() != null ? m.getStatus() : "SCHEDULED";
                    try {
                        if (m.getDate() != null) {
                            if (m.getDate().isBefore(today)) {
                                status = "COMPLETED";
                            } else if (m.getDate().isEqual(today)) {
                                if (m.getEndTime() != null && nowTime.isAfter(m.getEndTime())) {
                                    status = "COMPLETED";
                                } else if (m.getTime() != null && nowTime.isBefore(m.getTime())) {
                                    status = "UPCOMING";
                                } else {
                                    status = "ONGOING";
                                }
                            } else {
                                status = "UPCOMING";
                            }
                        }
                    } catch (Exception e) {
                        // fallback to persisted status
                    }

                    map.put("status", status);
                    return map;
                }).collect(Collectors.toList());
    }
}