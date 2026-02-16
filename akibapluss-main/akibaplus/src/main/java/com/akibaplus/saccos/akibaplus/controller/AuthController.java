package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.DTO.AuthRequests;
import com.akibaplus.saccos.akibaplus.model.User;
import com.akibaplus.saccos.akibaplus.repository.UserRepository;
import com.akibaplus.saccos.akibaplus.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.akibaplus.saccos.akibaplus.repository.MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequests.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequests.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRoles(java.util.Set.of(com.akibaplus.saccos.akibaplus.model.Role.ROLE_MEMBER));
        userRepository.save(user);

        // create associated Member record with default accounts
        com.akibaplus.saccos.akibaplus.model.Member member = new com.akibaplus.saccos.akibaplus.model.Member();
        // Split fullName into firstName and lastName
        String[] names = req.getFullName().split(" ", 2);
        member.setFirstName(names.length > 0 ? names[0] : "");
        member.setLastName(names.length > 1 ? names[1] : "");
        member.setPhone(req.getPhone());
        member.setNationalId(req.getNida());
        member.setMembershipNumber("MBR" + System.currentTimeMillis());
        member.setSavingsBalance(new java.math.BigDecimal("0.00"));
        member.setSharesValue(java.math.BigDecimal.ZERO);
        member.setStatus("ACTIVE");
        member.setUser(user);

        // save member via repository
        memberRepository.save(member);

        return ResponseEntity.ok(Map.of("message", "Registered"));
    }
}