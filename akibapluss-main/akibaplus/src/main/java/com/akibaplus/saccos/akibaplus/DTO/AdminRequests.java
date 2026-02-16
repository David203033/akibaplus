package com.akibaplus.saccos.akibaplus.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AdminRequests {
    public static class AddMemberRequest {
        @NotBlank
        private String fullName;
        private String phone;
        private String nationalId;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    }

    public static class RecordTransactionRequest {
        @NotBlank
        private String description;
        @NotNull
        private BigDecimal amount;

        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getType() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getType'");
        }
        public Long getMemberId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getMemberId'");
        }
    }
}