package com.akibaplus.saccos.akibaplus.service;

import com.akibaplus.saccos.akibaplus.DTO.LoanDTO;
import com.akibaplus.saccos.akibaplus.DTO.LoanEligibilityDTO;
import com.akibaplus.saccos.akibaplus.DTO.MemberProfileDTO;
import com.akibaplus.saccos.akibaplus.DTO.UpdateProfileRequest;
import com.akibaplus.saccos.akibaplus.model.Loan;
import com.akibaplus.saccos.akibaplus.model.Meeting;
import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.Notification;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import com.akibaplus.saccos.akibaplus.model.User;
import com.akibaplus.saccos.akibaplus.repository.LoanRepository;
import com.akibaplus.saccos.akibaplus.repository.MeetingRepository;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import com.akibaplus.saccos.akibaplus.repository.NotificationRepository;
import com.akibaplus.saccos.akibaplus.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    public MemberProfileDTO getMemberProfile(String email) {
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        MemberProfileDTO profile = new MemberProfileDTO();
        
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            profile.setFirstName(member.getFirstName());
            profile.setMiddleName(member.getMiddleName());
            profile.setLastName(member.getLastName());
            profile.setMembershipNumber(member.getMembershipNumber());
            profile.setJoinedOn(member.getJoinedOn() != null ? member.getJoinedOn() : LocalDate.now());
            profile.setStatus(member.getStatus());
            profile.setPhone(member.getPhone());
            profile.setDob(member.getDob());
            profile.setGender(member.getGender());
            profile.setMaritalStatus(member.getMaritalStatus());
            profile.setStreet(member.getStreet());
            profile.setDistrict(member.getDistrict());
            profile.setRegion(member.getRegion());
            profile.setAddressDescription(member.getAddressDescription());
            profile.setNextOfKinName(member.getNextOfKinName());
            profile.setNextOfKinPhone(member.getNextOfKinPhone());
            profile.setNextOfKinRelation(member.getNextOfKinRelation());
            profile.setNextOfKinPercent(member.getNextOfKinPercent());
            profile.setProfileImageUrl(member.getProfileImageUrl());
            profile.setUser(member.getUser());
        } else {
            // Fallback for safety, though controller handles auth
            User user = new User();
            user.setEmail(email);
            profile.setUser(user);
        }
        
        return profile;
    }

    public BigDecimal getSavingsBalance(String email) {
        return memberRepository.findByUser_Email(email)
                .map(Member::getSavingsBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getShareValue(String email) {
        return memberRepository.findByUser_Email(email)
                .map(Member::getSharesValue)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getLoanBalance(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return BigDecimal.ZERO;
        
        List<Loan> loans = loanRepository.findByMember(member.get());
        return loans.stream()
                .map(l -> l.getBalance() != null ? l.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<LoanDTO> getLoans(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return new ArrayList<>();

        List<LoanDTO> loans = new ArrayList<>();
        List<Loan> entityLoans = loanRepository.findByMember(member.get());
        
        for (Loan l : entityLoans) {
            BigDecimal amount = l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO;
            BigDecimal balance = l.getBalance() != null ? l.getBalance() : BigDecimal.ZERO;
            
            // Calculate progress
            int progress = 0;
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                progress = (int) ((amount.subtract(balance).doubleValue() / amount.doubleValue()) * 100);
            }
            
            // Estimate next payment (simple logic: balance / remaining months or fixed)
            BigDecimal nextPayment = BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                int remainingMonths = l.getDuration() > 0 ? l.getDuration() : 12; // Simplified
                nextPayment = balance.divide(new BigDecimal(remainingMonths), 0, java.math.RoundingMode.UP);
            }

            loans.add(new LoanDTO(
                l.getId(),
                l.getRef(),
                l.getType(),
                amount,
                balance,
                nextPayment,
                l.getDate(),
                l.getDate().plusMonths(l.getDuration()),
                l.getStatus(),
                progress
            ));
        }
        return loans;
    }

    public List<Map<String, Object>> getRecentTransactions(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> txns = new ArrayList<>();
        List<Transaction> entities = transactionRepository.findByMember(member.get());
        
        // Sort by date desc and limit
        entities.sort((t1, t2) -> {
            int dateCompare = t2.getDate().compareTo(t1.getDate());
            if (dateCompare == 0) {
                return Long.compare(t2.getId(), t1.getId());
            }
            return dateCompare;
        });
        
        for (Transaction t : entities) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", t.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            map.put("type", t.getType());
            map.put("description", t.getDescription());
            BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            BigDecimal balance = t.getBalance() != null ? t.getBalance() : BigDecimal.ZERO;
            map.put("amount", "TZS " + String.format("%,.0f", amount));
            map.put("balance", "TZS " + String.format("%,.0f", balance));
            map.put("status", t.getStatus());
            txns.add(map);
        }
        return txns;
    }

    public List<Map<String, Object>> getNotifications(String email) {
        // In a real app, filter by user or group. Fetching all for now or mock logic based on DB
        List<Map<String, Object>> notifs = new ArrayList<>();
        List<Notification> entities = notificationRepository.findAll(); // Should be findByRecipient...
        
        for (Notification n : entities) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", "Taarifa"); // Title not in entity, using generic
            map.put("desc", n.getMessage());
            map.put("color", "primary");
            map.put("icon", "fas fa-bell");
            notifs.add(map);
        }
        return notifs;
    }

    // Additional helper methods for other tabs
    public BigDecimal getInterestEarned(String email) { 
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return BigDecimal.ZERO;
        
        return transactionRepository.findByMember(member.get()).stream()
                .filter(t -> "RIBA".equalsIgnoreCase(t.getType()) || "INTEREST".equalsIgnoreCase(t.getType()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public int getDepositCount(String email) { 
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return 0;
        return (int) transactionRepository.findByMember(member.get()).stream()
                .filter(t -> "AMANA".equalsIgnoreCase(t.getType()))
                .count();
    }
    
    public int getShareCount(String email) { 
        BigDecimal val = getShareValue(email);
        return val.divideToIntegralValue(new BigDecimal("10000")).intValue(); // Assuming 10k per share
    }
    
    public BigDecimal getDividends(String email) { 
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return BigDecimal.ZERO;
        
        return transactionRepository.findByMember(member.get()).stream()
                .filter(t -> "GAWIO".equalsIgnoreCase(t.getType()) || "DIVIDEND".equalsIgnoreCase(t.getType()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getOwnershipPercentage(String email) { 
        BigDecimal myShares = getShareValue(email);
        // Calculate total shares from all members
        List<Member> allMembers = memberRepository.findAll();
        BigDecimal totalShares = allMembers.stream()
            .map(m -> m.getSharesValue() != null ? m.getSharesValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (totalShares.compareTo(BigDecimal.ZERO) == 0) return "0.0%";
        
        double percentage = myShares.divide(totalShares, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        return String.format("%.2f%%", percentage);
    }

    public List<Map<String, Object>> getShareTransactions(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return new ArrayList<>();

        List<Transaction> entities = transactionRepository.findByMember(member.get());
        
        return entities.stream()
            .filter(t -> "HISA".equalsIgnoreCase(t.getType()) || "SHARES".equalsIgnoreCase(t.getType()))
            .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", t.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                BigDecimal price = new BigDecimal("10000"); // Fixed price for now
                map.put("price", "TZS " + String.format("%,.0f", price));
                
                BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                // Use absolute value to show positive quantity and total cost
                BigDecimal absAmount = amount.abs();
                BigDecimal quantity = absAmount.divide(price, 0, java.math.RoundingMode.DOWN);
                
                map.put("quantity", quantity);
                map.put("total", "TZS " + String.format("%,.0f", absAmount));
                return map;
            })
            .collect(Collectors.toList());
    }

    // --- Analytics helpers ---
    public BigDecimal getTotalDeposits(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return BigDecimal.ZERO;
        return transactionRepository.findByMember(member.get()).stream()
                .filter(t -> "AMANA".equalsIgnoreCase(t.getType()) || "DEPOSIT".equalsIgnoreCase(t.getType()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCharges(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return BigDecimal.ZERO;
        return transactionRepository.findByMember(member.get()).stream()
                .filter(t -> {
                    String type = t.getType() == null ? "" : t.getType();
                    return ("TOZO".equalsIgnoreCase(type) || "CHARGE".equalsIgnoreCase(type) || "WITHDRAWAL".equalsIgnoreCase(type) || "FAINI".equalsIgnoreCase(type));
                })
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalTransactionsCount(String email) {
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return 0;
        return transactionRepository.findByMember(member.get()).size();
    }

    public Map<String, BigDecimal> getAssetAllocation(String email) {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("savings", getSavingsBalance(email));
        map.put("shares", getShareValue(email));
        map.put("loans", getLoanBalance(email));
        return map;
    }

    public List<Map<String, Object>> getSavingsHistory(String email, int months) {
        // Simple implementation: return last `months` points with current balance (can be improved later)
        List<Map<String, Object>> points = new ArrayList<>();
        BigDecimal current = getSavingsBalance(email);
        java.time.LocalDate now = java.time.LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            java.time.LocalDate dt = now.minusMonths(i);
            Map<String, Object> p = new HashMap<>();
            p.put("label", dt.getMonth().toString().substring(0,3) + " " + dt.getYear());
            p.put("value", current);
            points.add(p);
        }
        return points;
    }

    public Map<String, Object> getAnalyticsSummary(String email) {
        Map<String, Object> out = new HashMap<>();
        out.put("totalDeposits", getTotalDeposits(email));
        out.put("totalCharges", getTotalCharges(email));
        out.put("totalTransactions", getTotalTransactionsCount(email));
        out.put("assetAllocation", getAssetAllocation(email));
        out.put("savingsHistory", getSavingsHistory(email, 6));
        out.put("interestReceived", getInterestEarned(email));
        out.put("expectedDividend", getDividends(email));
        out.put("shareCount", getShareCount(email));
        out.put("creditScore", getCreditScore(email));
        return out;
    }
    
    public List<Map<String, Object>> getUpcomingMeetings() {
        List<Map<String, Object>> meetings = new ArrayList<>();
        List<Meeting> entities = meetingRepository.findAll();
        
        for (Meeting m : entities) {
            if (m.getDate() != null && !m.getDate().isBefore(LocalDate.now())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("name", m.getName());
                map.put("date", m.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                map.put("time", m.getTime() + (m.getEndTime() != null ? " - " + m.getEndTime() : ""));
                map.put("location", m.getLocation());
                map.put("desc", m.getDescription());
                
                // Add coordinates for attendance
                String latLng = m.getLatLng();
                if (latLng != null && latLng.contains(",")) {
                    String[] parts = latLng.split(",");
                    try {
                        map.put("latitude", Double.parseDouble(parts[0].trim()));
                        map.put("longitude", Double.parseDouble(parts[1].trim()));
                    } catch (Exception e) {
                        map.put("latitude", 0.0);
                        map.put("longitude", 0.0);
                    }
                } else {
                    map.put("latitude", 0.0);
                    map.put("longitude", 0.0);
                }
                
                meetings.add(map);
            }
        }
        return meetings;
    }

    public List<Map<String, Object>> getPastMeetings() {
        List<Map<String, Object>> meetings = new ArrayList<>();
        List<Meeting> entities = meetingRepository.findAll();
        
        for (Meeting m : entities) {
            if (m.getDate() != null && m.getDate().isBefore(LocalDate.now())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("name", m.getName());
                map.put("date", m.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                map.put("status", "Imepita"); // Logic to check attendance could go here
                meetings.add(map);
            }
        }
        return meetings;
    }

    public List<Map<String, Object>> getPenalties(String email) {
        // Fetch transactions of type 'FINE' or 'PENALTY'
        Optional<Member> member = memberRepository.findByUser_Email(email);
        if (member.isEmpty()) return new ArrayList<>();
        
        List<Transaction> txns = transactionRepository.findByMember(member.get());
        List<Map<String, Object>> penalties = new ArrayList<>();
        
        for (Transaction t : txns) {
            if ("FAINI".equalsIgnoreCase(t.getType()) || "PENALTY".equalsIgnoreCase(t.getType())) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", t.getDescription());
                map.put("date", t.getDate());
                map.put("amount", t.getAmount());
                map.put("desc", t.getDescription());
                map.put("status", t.getStatus()); // PENDING or PAID
                penalties.add(map);
            }
        }
        return penalties;
    }

    public BigDecimal getLoanLimit(String email) {
        // Logic: Savings * 3
        return getSavingsBalance(email).multiply(new BigDecimal("3"));
    }

    public LoanEligibilityDTO checkLoanEligibility(String email) {
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) {
            LoanEligibilityDTO eligibility = new LoanEligibilityDTO(false);
            eligibility.addReason("Mwanachama hayupo.");
            return eligibility;
        }
        Member member = memberOpt.get();
        LoanEligibilityDTO eligibility = new LoanEligibilityDTO(true);

        // i. Membership duration >= 3 months
        if (member.getJoinedOn() == null || ChronoUnit.MONTHS.between(member.getJoinedOn(), LocalDate.now()) < 3) {
            eligibility.addReason("Lazima uwe mwanachama kwa angalau miezi 3.");
        }

        // ii. Savings >= 50,000
        if (member.getSavingsBalance() == null || member.getSavingsBalance().compareTo(new BigDecimal("50000")) < 0) {
            eligibility.addReason("Lazima uwe na akiba ya angalau TZS 50,000.");
        }

        // iv & v. No outstanding unpaid loans
        long activeLoans = loanRepository.countByMemberAndStatusNotIn(member, List.of("PAID", "REJECTED", "CANCELLED"));
        if (activeLoans > 0) {
            eligibility.addReason("Una mkopo mwingine ambao haujakamilika. Maliza kwanza mkopo uliopo.");
        }

        // vi. Must have at least 3 shares
        BigDecimal sharesValue = member.getSharesValue() != null ? member.getSharesValue() : BigDecimal.ZERO;
        if (sharesValue.divide(new BigDecimal("10000")).compareTo(new BigDecimal("3")) < 0) {
            eligibility.addReason("Lazima uwe na angalau hisa 3.");
        }

        return eligibility;
    }

    /**
     * This method should be triggered by a daily scheduler (e.g., @Scheduled(cron = "0 0 1 * * ?")).
     * It finds overdue loans and applies a daily penalty.
     */
    public void applyDailyPenalties() {
        List<Loan> activeLoans = loanRepository.findByStatusNotIn(List.of("PAID", "REJECTED", "CANCELLED"));
        BigDecimal penaltyAmount = new BigDecimal("1000");
        LocalDate today = LocalDate.now();

        for (Loan loan : activeLoans) {
            LocalDate dueDate = loan.getDate().plusMonths(loan.getDuration());
            if (dueDate.isBefore(today)) {
                // Add penalty to loan balance or a separate penalty field
                loan.setBalance(loan.getBalance().add(penaltyAmount));
                loanRepository.save(loan);
                // Record penalty transaction
                recordTransaction(loan.getMember(), "FAINI", "Faini ya kuchelewa kulipa mkopo", penaltyAmount, loan.getMember().getSavingsBalance());
            }
        }
    }

    // --- Meeting Attendance & Fines Logic ---

    public void attendMeeting(String email, Long meetingId) {
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);

        if (memberOpt.isPresent() && meetingOpt.isPresent()) {
            Member member = memberOpt.get();
            Meeting meeting = meetingOpt.get();

            // Check if already attended
            boolean alreadyAttended = transactionRepository.findByMember(member).stream()
                .anyMatch(t -> "ATTENDANCE".equalsIgnoreCase(t.getType()) && t.getDescription().contains(meeting.getName()));
            
            if (alreadyAttended) return;

            // Record Attendance
            recordTransaction(member, "ATTENDANCE", "Mahudhurio: " + meeting.getName(), BigDecimal.ZERO, member.getSavingsBalance());

            // Check for Late Attendance (Grace period: 5 minutes)
            try {
                // meeting.getTime() is already a LocalTime
                LocalDateTime meetingStart = meeting.getDate().atTime(meeting.getTime());
                if (LocalDateTime.now().isAfter(meetingStart.plusMinutes(5))) {
                    // Apply Late Fine (2000 TZS)
                    recordFine(member, "FAINI", "Faini ya kuchelewa mkutano: " + meeting.getName(), new BigDecimal("2000"));
                }
            } catch (Exception e) {
                System.err.println("Error parsing meeting time for fine calculation: " + e.getMessage());
            }
        }
    }

    /**
     * Scheduled Task: Runs daily to check for absenteeism and overdue fines
     */
    public void processMeetingPenalties() {
        List<Meeting> meetings = meetingRepository.findAll();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 1. Check Absenteeism for yesterday's meetings
        for (Meeting m : meetings) {
            if (m.getDate() != null && m.getDate().equals(yesterday)) {
                List<Member> members = memberRepository.findAll();
                for (Member member : members) {
                    boolean attended = transactionRepository.findByMember(member).stream()
                        .anyMatch(t -> "ATTENDANCE".equalsIgnoreCase(t.getType()) && t.getDescription().contains(m.getName()));
                    
                    if (!attended) {
                        // Apply Absenteeism Fine (5000 TZS)
                        recordFine(member, "FAINI", "Faini ya kutohudhuria: " + m.getName(), new BigDecimal("5000"));
                    }
                }
            }
        }

        // 2. Deduct Overdue Fines (Unpaid for > 1 day)
        List<Transaction> transactions = transactionRepository.findAll();
        for (Transaction t : transactions) {
            if ("FAINI".equalsIgnoreCase(t.getType()) && "PENDING".equalsIgnoreCase(t.getStatus())) {
                if (t.getDate().isBefore(LocalDate.now().minusDays(1))) {
                    Member m = t.getMember();
                    BigDecimal currentSavings = m.getSavingsBalance() != null ? m.getSavingsBalance() : BigDecimal.ZERO;
                    
                    // Deduct from savings
                    m.setSavingsBalance(currentSavings.subtract(t.getAmount()));
                    memberRepository.save(m);
                    
                    t.setStatus("PAID"); // Mark as paid
                    t.setBalance(m.getSavingsBalance());
                    transactionRepository.save(t);
                }
            }
        }
    }

    public String getCreditScore(String email) { return "95%"; }
    
    public List<Map<String, Object>> getGuarantorRequests(String email) {
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getActiveGuarantees(String email) {
        return new ArrayList<>();
    }

    public Member updateProfile(Long id, UpdateProfileRequest req) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        member.setFullName(req.getFullName());
        member.setPhone(req.getPhone());
        // Update other fields
        return memberRepository.save(member);
    }

    public void deposit(String email, BigDecimal amount, String description) {
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            BigDecimal current = member.getSavingsBalance() != null ? member.getSavingsBalance() : BigDecimal.ZERO;
            BigDecimal newBalance = current.add(amount);
            
            member.setSavingsBalance(newBalance);
            memberRepository.save(member);
            
            recordTransaction(member, "AMANA", description, amount, newBalance);
            System.out.println("[MemberService] deposit(): email=" + email + " amount=" + amount + " description=" + description + " newBalance=" + newBalance);
        }
    }

    private void recordTransaction(Member member, String type, String desc, BigDecimal amount, BigDecimal balance) {
        Transaction txn = new Transaction();
        txn.setMember(member);
        txn.setType(type);
        txn.setDescription(desc);
        txn.setAmount(amount);
        txn.setBalance(balance);
        txn.setDate(LocalDate.now());
        txn.setStatus("COMPLETED");
        transactionRepository.save(txn);
    }

    private void recordFine(Member member, String type, String desc, BigDecimal amount) {
        Transaction txn = new Transaction();
        txn.setMember(member);
        txn.setType(type);
        txn.setDescription(desc);
        txn.setAmount(amount);
        txn.setBalance(member.getSavingsBalance()); // Balance doesn't change yet
        txn.setDate(LocalDate.now());
        txn.setStatus("PENDING"); // Mark as pending payment
        transactionRepository.save(txn);
    }

    public Map<String, Object> getAnalyticsData(String email) {
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) return new HashMap<>();
        Member member = memberOpt.get();
        
        List<Transaction> allTxns = transactionRepository.findByMember(member);
        LocalDate start2026 = LocalDate.of(2026, 1, 1);
        LocalDate end2026 = LocalDate.of(2026, 12, 31);

        // Filter transactions for 2026
        List<Transaction> txns2026 = allTxns.stream()
            .filter(t -> !t.getDate().isBefore(start2026) && !t.getDate().isAfter(end2026))
            .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        
        List<String> months = new ArrayList<>();
        List<BigDecimal> savingsTrend = new ArrayList<>();
        List<BigDecimal> sharesTrend = new ArrayList<>();
        List<BigDecimal> incomeTrend = new ArrayList<>();
        List<BigDecimal> expenseTrend = new ArrayList<>();
        List<BigDecimal> contributionsTrend = new ArrayList<>();
        List<BigDecimal> interestTrend = new ArrayList<>();
        List<BigDecimal> dividendsTrend = new ArrayList<>();

        // Calculate initial values before 2026
        BigDecimal runningShares = allTxns.stream()
            .filter(t -> t.getDate().isBefore(start2026))
            .filter(t -> "HISA".equalsIgnoreCase(t.getType()) || "SHARES".equalsIgnoreCase(t.getType()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal runningSavings = allTxns.stream()
            .filter(t -> t.getDate().isBefore(start2026))
            .max(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getId))
            .map(Transaction::getBalance)
            .orElse(BigDecimal.ZERO);

        for (int m = 1; m <= 12; m++) {
            LocalDate monthStart = LocalDate.of(2026, m, 1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            months.add(monthStart.getMonth().toString().substring(0, 3));

            List<Transaction> monthTxns = txns2026.stream()
                .filter(t -> !t.getDate().isBefore(monthStart) && !t.getDate().isAfter(monthEnd))
                .collect(Collectors.toList());

            // Savings Balance
            Optional<Transaction> lastTxn = monthTxns.stream()
                .max(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getId));
            if (lastTxn.isPresent()) runningSavings = lastTxn.get().getBalance();
            savingsTrend.add(runningSavings);

            // Shares Value
            BigDecimal sharesChange = monthTxns.stream()
                .filter(t -> "HISA".equalsIgnoreCase(t.getType()) || "SHARES".equalsIgnoreCase(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            runningShares = runningShares.add(sharesChange);
            sharesTrend.add(runningShares);

            // Income & Expenses
            incomeTrend.add(monthTxns.stream()
                .filter(t -> List.of("AMANA", "DEPOSIT", "RIBA", "INTEREST", "GAWIO", "DIVIDEND").contains(t.getType().toUpperCase()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            
            expenseTrend.add(monthTxns.stream()
                .filter(t -> List.of("TOZO", "WITHDRAWAL", "FAINI", "FINE").contains(t.getType().toUpperCase()))
                .map(t -> t.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add));

            contributionsTrend.add(monthTxns.stream().filter(t -> "AMANA".equalsIgnoreCase(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            interestTrend.add(monthTxns.stream().filter(t -> "RIBA".equalsIgnoreCase(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            dividendsTrend.add(monthTxns.stream().filter(t -> "GAWIO".equalsIgnoreCase(t.getType())).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        data.put("months", months);
        data.put("savingsTrend", savingsTrend);
        data.put("sharesTrend", sharesTrend);
        data.put("incomeTrend", incomeTrend);
        data.put("expenseTrend", expenseTrend);
        data.put("contributionsTrend", contributionsTrend);
        data.put("interestTrend", interestTrend);
        data.put("dividendsTrend", dividendsTrend);
        
        data.put("loanPaid", txns2026.stream().filter(t -> t.getType() != null && t.getType().contains("MALIPO_MKOPO")).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("loanOutstanding", getLoanBalance(email));
        data.put("currentSavings", getSavingsBalance(email));
        data.put("currentShares", getShareValue(email));
        data.put("currentLoans", getLoanBalance(email));

        return data;
    }
}