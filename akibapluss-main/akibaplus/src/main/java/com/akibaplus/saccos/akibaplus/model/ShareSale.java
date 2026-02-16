package com.akibaplus.saccos.akibaplus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "share_sales")
public class ShareSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Member seller;

    private int quantity;
    private BigDecimal pricePerShare;
    private LocalDate listedDate;
    private String status; // ACTIVE, SOLD, EXPIRED

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Member getSeller() { return seller; }
    public void setSeller(Member seller) { this.seller = seller; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPricePerShare() { return pricePerShare; }
    public void setPricePerShare(BigDecimal pricePerShare) { this.pricePerShare = pricePerShare; }
    public LocalDate getListedDate() { return listedDate; }
    public void setListedDate(LocalDate listedDate) { this.listedDate = listedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}