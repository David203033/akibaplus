package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}