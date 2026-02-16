package com.akibaplus.saccos.akibaplus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private String firstName;
    private String lastName;
    private String phone;
    private String membershipNumber;
    private String status;
    private String nationalId;
    private String profileImageUrl;
    
    private BigDecimal savingsBalance;
    private BigDecimal sharesValue;
    private LocalDate joinedOn;

    // New Profile Fields
    private String middleName;
    private LocalDate dob;
    private String gender;
    private String maritalStatus;
    
    private String street;
    private String district;
    private String region;
    private String addressDescription;
    
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;
    private int nextOfKinPercent;

    public Member() {
        this.savingsBalance = BigDecimal.ZERO;
        this.sharesValue = BigDecimal.ZERO;
        this.joinedOn = LocalDate.now();
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public BigDecimal getSavingsBalance() { return savingsBalance; }
    public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

    public BigDecimal getSharesValue() { return sharesValue; }
    public void setSharesValue(BigDecimal sharesValue) { this.sharesValue = sharesValue; }

    public LocalDate getJoinedOn() { return joinedOn; }
    public void setJoinedOn(LocalDate joinedOn) { this.joinedOn = joinedOn; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

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
    
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void setFullName(String fullName) {
        if (fullName == null) return;
        String[] names = fullName.split(" ", 2);
        this.firstName = names.length > 0 ? names[0] : "";
        this.lastName = names.length > 1 ? names[1] : "";
    }
}