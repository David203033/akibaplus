package com.akibaplus.saccos.akibaplus.service;

import com.akibaplus.saccos.akibaplus.DTO.MeetingPayload;
import com.akibaplus.saccos.akibaplus.model.Meeting;
import com.akibaplus.saccos.akibaplus.repository.MeetingRepository;
import com.akibaplus.saccos.akibaplus.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MemberRepository memberRepository;

    public Meeting scheduleNewMeeting(MeetingPayload payload) {
        Meeting meeting = new Meeting();
        meeting.setName(payload.getTitle());
        meeting.setDate(LocalDate.parse(payload.getDate()));
        meeting.setTime(LocalTime.parse(payload.getStartTime()));
        meeting.setEndTime(LocalTime.parse(payload.getEndTime()));
        meeting.setLocation(payload.getLocation());
        meeting.setLatLng(payload.getLatLng());
        meeting.setDescription(payload.getAgenda());
        meeting.setExpected((int) memberRepository.count());
        meeting.setAttended(0);
        meeting.setStatus("UPCOMING");

        return meetingRepository.save(meeting);
    }
}