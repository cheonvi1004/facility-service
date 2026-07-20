package com.infranics.iot.facility.repository;


import com.infranics.iot.facility.entity.DeviceStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceStatusLogRepository extends JpaRepository<DeviceStatusLog, Long> {
    // 특정 장비의 이력을 최신순으로 조회할 때 유용
    List<DeviceStatusLog> findBySensorNetworkUidOrderByReceivedAtDesc(String sensorNetworkUid);
}