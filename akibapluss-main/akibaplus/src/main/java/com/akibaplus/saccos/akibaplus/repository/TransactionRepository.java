package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.Member;
import com.akibaplus.saccos.akibaplus.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByMemberOrderByDateDesc(Member member);
    Page<Transaction> findByMember(Member member, Pageable pageable);
    List<Transaction> findByMember(Member member);
    List<Transaction> findByMemberAndType(Member member, String type);
}