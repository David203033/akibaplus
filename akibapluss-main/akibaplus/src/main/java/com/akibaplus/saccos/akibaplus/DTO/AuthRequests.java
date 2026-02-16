package com.akibaplus.saccos.akibaplus.DTO;

public interface AuthRequests {
    class LoginRequest {
        private String username;
        private String password;
        private String email;

        // Getters and setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
    
    class RegisterRequest {
        private String fullName;
        private String email;
        private String phone;
        private String nida;
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getNida() { return nida; }
        public void setNida(String nida) { this.nida = nida; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
