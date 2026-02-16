package com.akibaplus.saccos.akibaplus.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private LocalDate date;
    private LocalTime time;
    private LocalTime endTime;
    private String location;
    private String latLng;
    @Lob
    private String description;
    private String type;
    private Integer radius;
    private int expected;
    private int attended;
    private String status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLatLng() { return latLng; }
    public void setLatLng(String latLng) { this.latLng = latLng; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getRadius() { return radius; }
    public void setRadius(Integer radius) { this.radius = radius; }
    public int getExpected() { return expected; }
    public void setExpected(int expected) { this.expected = expected; }
    public int getAttended() { return attended; }
    public void setAttended(int attended) { this.attended = attended; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}