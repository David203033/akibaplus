package com.akibaplus.saccos.akibaplus.DTO;

public class MeetingPayload {
    private String title;
    private String date;
    private String startTime;
    private String endTime;
    private String location;
    private String latLng;
    private String agenda;

    // Getters
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getLocation() { return location; }
    public String getLatLng() { return latLng; }
    public String getAgenda() { return agenda; }
}