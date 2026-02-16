package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.ShareSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShareSaleRepository extends JpaRepository<ShareSale, Long> {
    List<ShareSale> findByStatus(String status);
}