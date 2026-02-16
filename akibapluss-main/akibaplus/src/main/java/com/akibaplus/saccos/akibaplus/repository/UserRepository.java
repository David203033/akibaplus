package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}