package com.yourorg.facility.normalize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.facility.device.DeviceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DeviceStatusNormalizerRegistry {

    private final Map<DeviceType, DeviceStatusNormalizer> normalizers = new EnumMap<>(DeviceType.class);

    public DeviceStatusNormalizerRegistry(List<DeviceStatusNormalizer> registered, ObjectMapper objectMapper) {
        // 명시적으로 등록된 정규화 구현체 우선 적용
        for (DeviceStatusNormalizer normalizer : registered) {
            normalizers.put(normalizer.supports(), normalizer);
        }
        // 나머지 장비종류는 fallback(범용) 구현으로 채움
        for (DeviceType type : DeviceType.values()) {
            normalizers.putIfAbsent(type, new DefaultDeviceStatusNormalizer(type, objectMapper));
        }
    }

    public DeviceStatusNormalizer get(DeviceType type) {
        return normalizers.get(type);
    }
}
