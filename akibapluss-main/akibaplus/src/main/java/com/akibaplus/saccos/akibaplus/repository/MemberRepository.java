package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUser(User user);
    Optional<Member> findByMembershipNumber(String membershipNumber);
    Optional<Member> findByUser_Email(String email);
}