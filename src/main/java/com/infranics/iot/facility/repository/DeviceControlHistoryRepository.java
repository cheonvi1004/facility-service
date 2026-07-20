package com.infranics.iot.facility.repository;


import com.infranics.iot.facility.entity.DeviceControlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceControlHistoryRepository extends JpaRepository<DeviceControlHistory, String> {
}