package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.DTO.NotificationPayload;
import com.akibaplus.saccos.akibaplus.model.Notification;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import com.akibaplus.saccos.akibaplus.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/notifications")
public class NotificationApiController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody NotificationPayload payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Notification notification = new Notification();
            notification.setMessage(payload.getMessage());
            notification.setRecipientGroup(payload.getRecipientGroup());
            notification.setSentAt(LocalDateTime.now());
            notification.setStatus("SENT");

            List<String> types = new ArrayList<>();
            if (payload.isSms()) types.add("SMS");
            if (payload.isApp()) types.add("APP");
            if (payload.isEmail()) types.add("EMAIL");
            notification.setType(String.join(", ", types));

            // Calculate recipient count based on group
            long count = 0;
            if (payload.getRecipientGroup() != null && payload.getRecipientGroup().toLowerCase().contains("wote")) {
                count = memberRepository.count();
            } else {
                count = 0; // Logic ya vikundi vingine inaweza kuongezwa hapa
            }
            notification.setRecipientCount((int) count);
            
            // Save notification to database
            notificationRepository.save(notification);

            response.put("success", true);
            response.put("message", "Arifa imetumwa kikamilifu!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Hitilafu: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}