package com.infranics.iot.facility.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "device_current_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceCurrentStatus {

    @Id
    @Column(name = "sensor_network_uid", length = 50)
    private String sensorNetworkUid;

    @Column(name = "api_key", length = 50, nullable = false)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20, nullable = false)
    private DeviceType deviceType;

    @Column(name = "cmd", length = 10)
    private String cmd;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_fields", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> normalizedFields;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode rawPayload;
}