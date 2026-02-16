package com.akibaplus.saccos.akibaplus.DTO;

import com.akibaplus.saccos.akibaplus.model.User;
import java.time.LocalDate;

public class MemberProfileDTO {
    private String firstName;
    private String middleName;
    private String lastName;
    private String membershipNumber;
    private LocalDate joinedOn;
    private String status;
    private LocalDate dob;
    private String gender;
    private String maritalStatus;
    private String phone;
    private String street;
    private String district;
    private String region;
    private String addressDescription;
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;
    private int nextOfKinPercent;
    private String profileImageUrl;
    private User user; // To support profile.user.email

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }
    public LocalDate getJoinedOn() { return joinedOn; }
    public void setJoinedOn(LocalDate joinedOn) { this.joinedOn = joinedOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAddressDescription() { return addressDescription; }
    public void setAddressDescription(String addressDescription) { this.addressDescription = addressDescription; }
    public String getNextOfKinName() { return nextOfKinName; }
    public void setNextOfKinName(String nextOfKinName) { this.nextOfKinName = nextOfKinName; }
    public String getNextOfKinPhone() { return nextOfKinPhone; }
    public void setNextOfKinPhone(String nextOfKinPhone) { this.nextOfKinPhone = nextOfKinPhone; }
    public String getNextOfKinRelation() { return nextOfKinRelation; }
    public void setNextOfKinRelation(String nextOfKinRelation) { this.nextOfKinRelation = nextOfKinRelation; }
    public int getNextOfKinPercent() { return nextOfKinPercent; }
    public void setNextOfKinPercent(int nextOfKinPercent) { this.nextOfKinPercent = nextOfKinPercent; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}