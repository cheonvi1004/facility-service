package com.infranics.iot.facility.entity;


import com.infranics.iot.facility.device.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device_control_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceControlHistory {

    @Id
    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "sensor_network_uid", length = 50, nullable = false)
    private String sensorNetworkUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20, nullable = false)
    private DeviceType deviceType;

    @Column(name = "cmd", length = 10, nullable = false)
    private String cmd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "control_data", columnDefinition = "jsonb")
    private Object controlData;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // PENDING, SUCCESS, TIMEOUT, ERROR

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "expected_response_cmd", length = 10)
    private String expectedResponseCmd;
}