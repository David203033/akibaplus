package com.akibaplus.saccos.akibaplus.controller;

import com.akibaplus.saccos.akibaplus.DTO.MeetingPayload;
import com.akibaplus.saccos.akibaplus.model.Meeting;
import com.akibaplus.saccos.akibaplus.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/meetings")
public class MeetingApiController {

    @Autowired
    private MeetingService meetingService;

    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> scheduleMeeting(@RequestBody MeetingPayload payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Meeting newMeeting = meetingService.scheduleNewMeeting(payload);

            Map<String, Object> meetingData = new HashMap<>();
            meetingData.put("name", newMeeting.getName());
            meetingData.put("date", newMeeting.getDate().toString());
            meetingData.put("time", newMeeting.getTime().toString());
            meetingData.put("location", newMeeting.getLocation());
            meetingData.put("expected", newMeeting.getExpected());
            meetingData.put("attended", newMeeting.getAttended());
            meetingData.put("status", newMeeting.getStatus());

            response.put("success", true);
            response.put("message", "Mkutano umepangwa na kuhifadhiwa kikamilifu!");
            response.put("newMeeting", meetingData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Kuna tatizo la kiufundi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}