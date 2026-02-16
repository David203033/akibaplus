package com.akibaplus.saccos.akibaplus.service;

import com.akibaplus.saccos.akibaplus.DTO.AdminRequests;
import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import com.akibaplus.saccos.akibaplus.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class AdminService {
    public final MemberRepository memberRepository;
    public final TransactionRepository transactionRepository;

    public AdminService(MemberRepository memberRepository, TransactionRepository transactionRepository) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
    }

    public Member addMember(AdminRequests.AddMemberRequest req) {
        Member member = new Member();
        member.setFullName(req.getFullName());
        member.setPhone(req.getPhone());
        member.setNationalId(req.getNationalId());
        return memberRepository.save(member);
    }

    public Transaction recordTransaction(AdminRequests.RecordTransactionRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + req.getMemberId()));

        Transaction transaction = new Transaction();
        transaction.setMember(member);
        transaction.setType(req.getType());
        transaction.setDescription(req.getDescription());
        transaction.setAmount(req.getAmount());
        transaction.setDate(LocalDate.now());
        transaction.setStatus("POSTED");

        BigDecimal current = member.getSavingsBalance() != null ? member.getSavingsBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = current;
        if ("CREDIT".equalsIgnoreCase(req.getType()) || "AMANA".equalsIgnoreCase(req.getType()) || "DEPOSIT".equalsIgnoreCase(req.getType())) {
            newBalance = current.add(req.getAmount());
        } else if ("DEBIT".equalsIgnoreCase(req.getType()) || "WITHDRAWAL".equalsIgnoreCase(req.getType()) || "TOZO".equalsIgnoreCase(req.getType()) || "FAINI".equalsIgnoreCase(req.getType())) {
            newBalance = current.subtract(req.getAmount());
        }
        
        // Update Shares Value if transaction is for Shares
        if ("HISA".equalsIgnoreCase(req.getType()) || "SHARES".equalsIgnoreCase(req.getType())) {
            BigDecimal currentShares = member.getSharesValue() != null ? member.getSharesValue() : BigDecimal.ZERO;
            member.setSharesValue(currentShares.add(req.getAmount()));
        }

        transaction.setBalance(newBalance);

        Transaction saved = transactionRepository.save(transaction);
        member.setSavingsBalance(newBalance);
        memberRepository.save(member);

        return saved;
    }

    /**
     * Dashboard Statistics Methods
     */
    
    public long getTotalMembers() {
        return memberRepository.count();
    }
    
    public double getTotalSavings() {
        // Sum savingsBalance for all members
        List<Member> members = memberRepository.findAll();
        BigDecimal total = members.stream()
                .map(m -> m.getSavingsBalance() != null ? m.getSavingsBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.doubleValue();
    }
    
    public long getActiveLoanCount() {
        // placeholder: implement with LoanRepository if available
        return 0L;
    }
    
    public long getPendingFinesCount() {
        return 0L;
    }
    
    public List<Transaction> getRecentActivities() {
        List<Transaction> txs = transactionRepository.findAll();
        return txs.stream()
                .sorted(Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * Member Management Methods
     */
    
    public double getAverageSavings() {
        double total = getTotalSavings();
        long count = getTotalMembers();
        return count > 0 ? total / count : 0.0;
    }
    
    public double getTotalInterestEarned() {
        // placeholder
        return 0.0;
    }
    
    /**
     * Expose list of members
     */
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * Loans Management Methods
     */
    
    public List<?> getAllLoans() {
        // Fetch all loans from database
        return Collections.emptyList();
    }
    
    public void approveLoan(Long loanId) {
        //  Update loan status to APPROVED
    }
    
    public void rejectLoan(Long loanId) {
        //  Update loan status to REJECTED
    }
    
    /**
     * Shares & Dividends Methods
     */
    
    public long getTotalSharesPurchased() {
        // Sum all shares purchased
        return 0L;
    }
    
    public double getTotalSharesValue() {
        //  Calculate total value of shares
        return 0.0;
    }
    
    public double getDeclaredDividend() {
        // : Get latest declared dividend amount
        return 0.0;
    }
    
    public double getPreviousYearDividend() {
        // : Get dividend from previous year
        return 0.0;
    }
    
    public void approveDividend(Double amount) {
        // : Create dividend record and approve
    }
    
    /**
     * Expenses Management Methods
     */
    
    public List<?> getExpensesList() {
        // : Fetch all expenses from database
        return Collections.emptyList();
    }
    
    /**
     * Fines Management Methods
     */
    
    public List<?> getFinesList() {
        //: Fetch all fines with member details
        return Collections.emptyList();
    }
    
    public void recordFinePayment(Long fineId, Double amount) {
        //: Record fine payment in database
    }
    
    /**
     * Meetings Management Methods
     */
    
    public List<?> getUpcomingMeetings() {
        //  Fetch meetings with date > today
        return Collections.emptyList();
    }
    
    public void scheduleMeeting(String title, String date, String location) {
        // : Create new meeting record
    }
    
    /**
     * Notifications Methods
     */
    
    public void sendNotification(String message, String recipients) {
        // : Send notification via SMS/Email/App
        // - Parse recipients parameter
        // - Send via appropriate channels
        // - Log notification in database
    }
    
    /**
     * Audit Logs Methods
     */
    
    public List<?> getAuditLogs() {
        // : Fetch audit logs from database, sorted by date DESC
        return Collections.emptyList();
    }
    
    /**
     * Users Management Methods
     */
    
    public List<?> getSystemUsers() {
        // : Fetch all system users with roles
        return Collections.emptyList();
    }
    
    /**
     * Reports Generation Methods
     */
    
    public String generateReport(String type) {
        switch(type) {
            case "Wanachama":
                return generateMembersReport();
            case "Akiba":
                return generateSavingsReport();
            case "Mikopo":
                return generateLoansReport();
            default:
                return null;
        }
    }
    
    private String generateMembersReport() {
        // : Generate members report in PDF/Excel
        return "/reports/members_" + System.currentTimeMillis() + ".pdf";
    }
    
    private String generateSavingsReport() {
        // : Generate savings report
        return "/reports/savings_" + System.currentTimeMillis() + ".pdf";
    }
    
    private String generateLoansReport() {
        // : Generate loans report
        return "/reports/loans_" + System.currentTimeMillis() + ".pdf";
    }
    
    /**
     * Analytics Methods
     */
    
    public Map<String, Object> getDashboardChartData() {
        Map<String, Object> data = new HashMap<>();
        
        // Growth chart data
        data.put("growthData", getGrowthChartData());
        
        // Portfolio chart data
        data.put("portfolioData", getPortfolioChartData());
        
        return data;
    }
    
    private List<?> getGrowthChartData() {
        // : Calculate monthly growth percentages
        return Collections.emptyList();
    }
    
    private List<?> getPortfolioChartData() {
        //: Calculate portfolio distribution (savings, loans, dividends, shares)
        return Collections.emptyList();
    }
}