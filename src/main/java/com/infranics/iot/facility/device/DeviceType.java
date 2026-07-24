package com.infranics.iot.facility.device;

import java.util.Arrays;

/**
 * 문서 4.1절 기준 설비 종류.
 * 문서 표에는 6종만 명시되어 있고 제목은 "7종"이므로,
 * 7번째 설비명은 발주처 확인 후 추가 필요.
 */
public enum DeviceType {

    LIGHT("Light"),
    FIRE("Fire"),
    LOUVER("louver"),
    PUMP("pump"),
    ACCESS("access"),
    INTERPHONE("interphone");

    private final String topicName;

    DeviceType(String topicName) {
        this.topicName = topicName;
    }

    public String topicName() {
        return topicName;
    }

    public static DeviceType fromTopicName(String name) {
        return Arrays.stream(values())
                .filter(t -> t.topicName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown device topic segment: " + name));
    }
}
