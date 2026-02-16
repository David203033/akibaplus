package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.Loan;
import com.akibaplus.saccos.akibaplus.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByMember(Member member);
    
    long countByMemberAndStatusNotIn(Member member, List<String> statuses);

    List<Loan> findByStatusNotIn(List<String> statuses);

    Optional<Loan> findByRef(String ref);
}