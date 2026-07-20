package com.infranics.iot.facility.entity;

import com.infranics.iot.facility.device.DeviceType;
import com.infranics.iot.facility.call.CallSessionState;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "call_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallHistory {

    @Id
    @Column(name = "call_id", length = 50)
    private String callId;

    @Column(name = "api_key", length = 50, nullable = false)
    private String apiKey;

    @Column(name = "sensor_network_uid", length = 50, nullable = false)
    private String sensorNetworkUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20, nullable = false)
    private DeviceType deviceType;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_state", length = 20, nullable = false)
    private CallSessionState endState;

    @Column(name = "end_reason", length = 50, nullable = false)
    private String endReason;
}