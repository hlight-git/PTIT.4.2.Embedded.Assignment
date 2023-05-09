package com.example.subcribethread.repository;

import com.example.subcribethread.model.StatusIot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusIotRepository extends JpaRepository<StatusIot, Long> {
    @Query("SELECT si from StatusIot si order by si.id desc")
    List<StatusIot> getAllOrderByIdDesc();
}
