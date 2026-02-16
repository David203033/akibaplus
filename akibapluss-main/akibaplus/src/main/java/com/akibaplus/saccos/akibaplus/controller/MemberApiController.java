package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.model.Loan;
import com.akibaplus.saccos.akibaplus.model.Meeting;
import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.ShareSale;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import com.akibaplus.saccos.akibaplus.model.User;
import com.akibaplus.saccos.akibaplus.repository.LoanRepository;
import com.akibaplus.saccos.akibaplus.repository.MeetingRepository;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import com.akibaplus.saccos.akibaplus.repository.ShareSaleRepository;
import com.akibaplus.saccos.akibaplus.repository.TransactionRepository;
import com.akibaplus.saccos.akibaplus.service.MemberService;
import com.akibaplus.saccos.akibaplus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class MemberApiController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ShareSaleRepository shareSaleRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MeetingRepository meetingRepository;

    // 1. Shares (Hisa)
    @PostMapping("/shares/buy")
    public ResponseEntity<?> buyShares(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        }
        
        Member member = memberOpt.get();
        
        try {
            // Default to 1 if not provided
            int quantity = payload.containsKey("quantity") ? Integer.parseInt(payload.get("quantity").toString()) : 1;
            BigDecimal pricePerShare = new BigDecimal("10000"); // Fixed price
            BigDecimal cost = pricePerShare.multiply(new BigDecimal(quantity));
            
            BigDecimal currentSavings = member.getSavingsBalance() != null ? member.getSavingsBalance() : BigDecimal.ZERO;
            
            if (currentSavings.compareTo(cost) < 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Salio la akiba haitoshi."));
            }
            
            // Deduct from savings
            member.setSavingsBalance(currentSavings.subtract(cost));
            
            // Add to shares value
            BigDecimal currentSharesVal = member.getSharesValue() != null ? member.getSharesValue() : BigDecimal.ZERO;
            member.setSharesValue(currentSharesVal.add(cost));
            
            memberRepository.save(member);
            recordTransaction(member, "HISA", "Ununuzi wa Hisa (" + quantity + ")", cost.negate(), member.getSavingsBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Umefanikiwa kununua hisa " + quantity + ".");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    @PostMapping("/shares/sell")
    public ResponseEntity<?> sellShares(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        Member member = memberOpt.get();

        try {
            int quantity = Integer.parseInt(payload.get("quantity").toString());
            BigDecimal price = new BigDecimal(payload.get("price").toString());
            BigDecimal totalValue = price.multiply(new BigDecimal(quantity));

            BigDecimal currentShares = member.getSharesValue() != null ? member.getSharesValue() : BigDecimal.ZERO;
            
            // Deduct shares immediately (Escrow)
            member.setSharesValue(currentShares.subtract(totalValue));
            memberRepository.save(member);
            
            // Create Market Entry
            ShareSale sale = new ShareSale();
            sale.setSeller(member);
            sale.setQuantity(quantity);
            sale.setPricePerShare(price);
            sale.setListedDate(LocalDate.now());
            sale.setStatus("ACTIVE");
            shareSaleRepository.save(sale);
            
            recordTransaction(member, "HISA", "Hisa zimewekwa sokoni (" + quantity + ")", BigDecimal.ZERO, member.getSavingsBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Hisa zimeuzwa na kiasi cha TZS " + totalValue + " kimeongezwa kwenye akiba.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    @PostMapping("/shares/buy-market")
    public ResponseEntity<?> buyMarketShare(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> buyerOpt = memberRepository.findByUser_Email(email);
        if (buyerOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        Member buyer = buyerOpt.get();

        try {
            Long saleId = Long.parseLong(payload.get("saleId").toString());
            Optional<ShareSale> saleOpt = shareSaleRepository.findById(saleId);
            if(saleOpt.isEmpty() || !saleOpt.get().getStatus().equals("ACTIVE")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hisa hizi hazipo sokoni."));
            }
            ShareSale sale = saleOpt.get();
            
            BigDecimal cost = sale.getPricePerShare().multiply(new BigDecimal(sale.getQuantity()));
            if (buyer.getSavingsBalance().compareTo(cost) < 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Salio la akiba haitoshi."));
            }

            // Process Buyer
            buyer.setSavingsBalance(buyer.getSavingsBalance().subtract(cost));
            buyer.setSharesValue(buyer.getSharesValue().add(cost));
            memberRepository.save(buyer);
            recordTransaction(buyer, "HISA", "Ununuzi wa Hisa Sokoni (" + sale.getQuantity() + ")", cost.negate(), buyer.getSavingsBalance());

            // Process Seller
            Member seller = sale.getSeller();
            seller.setSavingsBalance(seller.getSavingsBalance().add(cost));
            memberRepository.save(seller);
            recordTransaction(seller, "HISA", "Hisa Zimeuzwa Sokoni (" + sale.getQuantity() + ")", cost, seller.getSavingsBalance());

            sale.setStatus("SOLD");
            shareSaleRepository.save(sale);

            return ResponseEntity.ok(Map.of("success", true, "message", "Umefanikiwa kununua hisa."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    // 2. Loans (Mikopo)
    @PostMapping("/loans/apply")
    public ResponseEntity<?> applyLoan(
            @RequestParam Map<String, String> payload,
            @RequestParam(value = "idDoc", required = false) MultipartFile idDoc,
            @RequestParam(value = "passportPhoto", required = false) MultipartFile passportPhoto,
            @RequestParam(value = "collateralDoc", required = false) MultipartFile collateralDoc,
            @RequestParam(value = "guarantorDoc", required = false) MultipartFile guarantorDoc,
            Authentication authentication) {

        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        Member member = memberOpt.get();

        // Re-verify eligibility on the server-side
        var eligibility = memberService.checkLoanEligibility(email);
        if (!eligibility.isEligible()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hustahiki kuomba mkopo: " + String.join(", ", eligibility.getReasons())));
        }

        try {
            // Since it's auto-approved, create the Loan directly
            Loan loan = new Loan();
            loan.setMember(member);
            loan.setAmount(new BigDecimal(payload.get("amount").toString()));
            loan.setType(payload.get("type"));
            loan.setDuration(Integer.parseInt(payload.get("duration").toString()));
            loan.setReason(payload.get("reason"));
            loan.setStatus("ACTIVE"); // Auto-approved as per requirements
            loan.setDate(LocalDate.now());
            loan.setRef("L" + System.currentTimeMillis());
            loan.setBalance(loan.getAmount()); // Initial balance is the full amount
            
            loanRepository.save(loan);

            // Record disbursement transaction
            recordTransaction(member, "MKOPO", "Mkopo Umeidhinishwa: " + loan.getType(), loan.getAmount(), member.getSavingsBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ombi lako la mkopo limeidhinishwa na kuchakatwa kikamilifu!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    @PostMapping("/loans/pay")
    public ResponseEntity<?> payLoan(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        Member member = memberOpt.get();

        try {
            // Tumia loanId badala ya ref kwa uhakika zaidi
            Long tempLoanId = null;
            if (payload.containsKey("loanId") && payload.get("loanId") != null && !payload.get("loanId").toString().isEmpty()) {
                tempLoanId = Long.parseLong(payload.get("loanId").toString());
            }
            final Long loanId = tempLoanId;
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String method = (String) payload.get("method"); // BANK or MOBILE
            
            // Find loan manually to ensure safety
            List<Loan> loans = loanRepository.findByMember(member);
            Optional<Loan> targetLoan;
            
            if (loanId != null) {
                targetLoan = loans.stream().filter(l -> l.getId().equals(loanId)).findFirst();
            } else {
                String ref = (String) payload.get("ref");
                targetLoan = loans.stream().filter(l -> l.getRef() != null && l.getRef().equals(ref)).findFirst();
            }
            
            if (targetLoan.isEmpty()) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mkopo haukupatikana."));
            }
            Loan loan = targetLoan.get();
            
            BigDecimal currentBalance = loan.getBalance();
            // Ulinzi: Kama salio ni null (kwa mkopo mpya), tumia kiasi cha msingi cha mkopo kama salio.
            if (currentBalance == null) {
                currentBalance = loan.getAmount() != null ? loan.getAmount() : BigDecimal.ZERO;
            }

            BigDecimal newBalance = currentBalance.subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            
            loan.setBalance(newBalance);
            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                loan.setStatus("PAID");
            }
            loanRepository.save(loan);
            
            recordTransaction(member, "MALIPO_MKOPO", "Malipo ya Mkopo " + loan.getType() + " (" + method + ")", amount, member.getSavingsBalance());
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Malipo ya TZS " + amount + " yamepokelewa. Salio jipya la mkopo: " + newBalance));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    // 3. Savings (Akiba - Amana na Kutoa)
    @PostMapping("/savings/deposit")
    public ResponseEntity<?> depositSavings(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        }
        
        Member member = memberOpt.get();
        
        try {
            System.out.println("[MemberApiController] depositSavings called for: " + email + " payload=" + payload);
            if (!payload.containsKey("amount")) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Kiasi hakipo"));
            }
            
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Kiasi lazima kiwe zaidi ya 0"));
            }
            
            BigDecimal current = member.getSavingsBalance() != null ? member.getSavingsBalance() : BigDecimal.ZERO;
            member.setSavingsBalance(current.add(amount));
            
            memberRepository.save(member);
            recordTransaction(member, "AMANA", "Amana ya Fedha", amount, member.getSavingsBalance());
            System.out.println("[MemberApiController] deposit recorded: memberId=" + member.getId() + " newBalance=" + member.getSavingsBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Amana ya TZS " + amount + " imepokelewa na salio limesasishwa.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    @PostMapping("/savings/withdraw")
    public ResponseEntity<?> withdrawSavings(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        }
        
        Member member = memberOpt.get();
        
        try {
            // Rule: No withdrawal until November 30th
            LocalDate today = LocalDate.now();
            if (today.isBefore(LocalDate.of(today.getYear(), 11, 30))) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Huwezi kutoa fedha hadi tarehe 30 Novemba."));
            }

            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            BigDecimal current = member.getSavingsBalance() != null ? member.getSavingsBalance() : BigDecimal.ZERO;
            
            if (current.compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Salio halitoshi."));
            }
            
            member.setSavingsBalance(current.subtract(amount));
            memberRepository.save(member);
            recordTransaction(member, "TOZO", "Kutoa Fedha", amount.negate(), member.getSavingsBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ombi la kutoa fedha limekubaliwa. Salio jipya: " + member.getSavingsBalance());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    // 4. Penalties (Faini)
    @PostMapping("/penalties/pay")
    public ResponseEntity<?> payPenalty(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        Member member = memberOpt.get();

        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            // Deduct from savings
            member.setSavingsBalance(member.getSavingsBalance().subtract(amount));
            memberRepository.save(member);
            
            recordTransaction(member, "FAINI", "Malipo ya Faini", amount.negate(), member.getSavingsBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Malipo ya faini yamepokelewa.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    // 5. Profile Update
    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @RequestParam Map<String, String> payload,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {
        
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        }
        
        Member member = memberOpt.get();
        
        if (payload.containsKey("phone")) member.setPhone((String) payload.get("phone"));
        if (payload.containsKey("firstName")) member.setFirstName((String) payload.get("firstName"));
        if (payload.containsKey("lastName")) member.setLastName((String) payload.get("lastName"));
        if (payload.containsKey("middleName")) member.setMiddleName((String) payload.get("middleName"));
        if (payload.containsKey("dob") && !payload.get("dob").isEmpty()) member.setDob(LocalDate.parse((String) payload.get("dob")));
        if (payload.containsKey("gender")) member.setGender((String) payload.get("gender"));
        if (payload.containsKey("maritalStatus")) member.setMaritalStatus((String) payload.get("maritalStatus"));
        
        // Address Fields
        if (payload.containsKey("street")) member.setStreet((String) payload.get("street"));
        if (payload.containsKey("district")) member.setDistrict((String) payload.get("district"));
        if (payload.containsKey("region")) member.setRegion((String) payload.get("region"));
        if (payload.containsKey("addressDescription")) member.setAddressDescription((String) payload.get("addressDescription"));
        
        // Next of Kin Fields
        if (payload.containsKey("nextOfKinName")) member.setNextOfKinName((String) payload.get("nextOfKinName"));
        if (payload.containsKey("nextOfKinPhone")) member.setNextOfKinPhone((String) payload.get("nextOfKinPhone"));
        if (payload.containsKey("nextOfKinRelation")) member.setNextOfKinRelation((String) payload.get("nextOfKinRelation"));
        if (payload.containsKey("nextOfKinPercent") && !payload.get("nextOfKinPercent").isEmpty()) {
            member.setNextOfKinPercent(Integer.parseInt(payload.get("nextOfKinPercent").toString()));
        }

        // Handle Profile Image Upload
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Define upload directory (e.g., static/uploads or external storage)
                // For simplicity, we'll use a relative path in static resources or a temp folder mapped to resource handler
                // In production, use a configured path
                String uploadDir = "src/main/resources/static/uploads/profile/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                String fileName = member.getId() + "_" + System.currentTimeMillis() + "_" + profileImage.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(profileImage.getInputStream(), filePath);
                
                // Save relative path to DB (accessible via /uploads/profile/...)
                member.setProfileImageUrl("/uploads/profile/" + fileName);
                recordTransaction(member, "PROFILE", "Picha ya wasifu imesasishwa", BigDecimal.ZERO, member.getSavingsBalance());
                
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Imeshindwa kupakia picha: " + e.getMessage()));
            }
        }

        memberRepository.save(member);
        
        recordTransaction(member, "PROFILE", "Taarifa za wasifu zimesasishwa", BigDecimal.ZERO, member.getSavingsBalance());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Taarifa za profaili zimesasishwa kikamilifu.");
        if (member.getProfileImageUrl() != null) {
            response.put("imageUrl", member.getProfileImageUrl());
        }
        return ResponseEntity.ok(response);
    }
    
    // 6. Password Change
    @PostMapping("/profile/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }
        
        User user = userOpt.get();
        String newPassword = (String) payload.get("newPassword");
        
        if (newPassword == null || newPassword.length() < 6) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nenosiri lazima liwe na herufi 6 au zaidi."));
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isPresent()) {
            recordTransaction(memberOpt.get(), "USALAMA", "Nenosiri limebadilishwa", BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Nenosiri limebadilishwa kikamilifu.");
        return ResponseEntity.ok(response);
    }

    // 7. Meeting Attendance
    @PostMapping("/meetings/attend")
    public ResponseEntity<?> signAttendance(@RequestBody Map<String, Object> payload, Authentication authentication) {
        String email = authentication.getName();
        Optional<Member> memberOpt = memberRepository.findByUser_Email(email);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Member not found"));
        }

        try {
            Long meetingId = Long.parseLong(payload.get("meetingId").toString());
            double userLat = Double.parseDouble(payload.get("latitude").toString());
            double userLng = Double.parseDouble(payload.get("longitude").toString());

            Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
            if (meetingOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mkutano haukupatikana."));
            }

            Meeting meeting = meetingOpt.get();
            
            // Check if meeting is active/today
            if (meeting.getDate().isBefore(LocalDate.now())) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mkutano huu umeshapita."));
            }

            // Parse meeting location
            String latLng = meeting.getLatLng();
            if (latLng == null || !latLng.contains(",")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Eneo la mkutano halijawekwa."));
            }

            String[] parts = latLng.split(",");
            double meetingLat = Double.parseDouble(parts[0].trim());
            double meetingLng = Double.parseDouble(parts[1].trim());

            double distance = calculateDistance(userLat, userLng, meetingLat, meetingLng);
            
            // Allow 200 meters radius
            if (distance > 200) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Uko mbali na eneo la mkutano (" + (int)distance + "m). Tafadhali sogea karibu."));
            }

            // Update attendance count
            meeting.setAttended(meeting.getAttended() + 1);
            meetingRepository.save(meeting);
            
            // Optional: Record this specific member's attendance in a separate table if needed in future

            return ResponseEntity.ok(Map.of("success", true, "message", "Mahudhurio yamerekodiwa kikamilifu!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Hitilafu: " + e.getMessage()));
        }
    }

    private Transaction recordTransaction(Member member, String type, String desc, BigDecimal amount, BigDecimal balance) {
        Transaction txn = new Transaction();
        txn.setMember(member);
        txn.setType(type);
        txn.setDescription(desc);
        txn.setAmount(amount);
        txn.setBalance(balance);
        txn.setDate(LocalDate.now());
        txn.setStatus("COMPLETED");
        return transactionRepository.save(txn);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }
}