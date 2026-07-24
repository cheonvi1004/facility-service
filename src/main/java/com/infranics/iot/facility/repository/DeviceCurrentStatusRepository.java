package com.infranics.iot.facility.repository;


import com.infranics.iot.facility.entity.DeviceCurrentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceCurrentStatusRepository extends JpaRepository<DeviceCurrentStatus, String> {
}