package com.example.subcribethread.repository;

import com.example.subcribethread.model.ControlState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlStateRepository extends JpaRepository<ControlState, Long> {
    @Query("SELECT cs from ControlState cs order by cs.id desc")
    List<ControlState> getAllOrderByIdDesc();
}
