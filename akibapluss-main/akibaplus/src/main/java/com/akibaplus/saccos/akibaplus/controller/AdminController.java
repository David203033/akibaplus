package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.DTO.AdminRequests;
import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import com.akibaplus.saccos.akibaplus.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/members")
    public ResponseEntity<Member> addMember(@Valid @RequestBody AdminRequests.AddMemberRequest req) {
        Member member = adminService.addMember(req);
        return ResponseEntity.ok(member);
    }

    @GetMapping("/members")
    public ResponseEntity<List<Member>> getAllMembers() {
        List<Member> members = adminService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> recordTransaction(@Valid @RequestBody AdminRequests.RecordTransactionRequest req) {
        Transaction transaction = adminService.recordTransaction(req);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = adminService.getDashboardChartData();
        return ResponseEntity.ok(stats);
    }
}