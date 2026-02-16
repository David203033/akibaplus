package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> updateProfile(@PathVariable Long id, @Valid @RequestBody com.akibaplus.saccos.akibaplus.DTO.UpdateProfileRequest req) {
        Member member = memberService.updateProfile(id, req);
        return ResponseEntity.ok(member);
    }
}