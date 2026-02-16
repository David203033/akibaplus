package com.akibaplus.saccos.akibaplus.repository;

import com.akibaplus.saccos.akibaplus.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}