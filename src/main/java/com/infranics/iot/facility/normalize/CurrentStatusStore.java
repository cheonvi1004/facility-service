package com.infranics.iot.facility.normalize;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 장비 UID별 최신 상태를 메모리에 보관.
 * web이 WebSocket에 새로 접속할 때 이 스냅샷을 INITIAL_SNAPSHOT으로 전달한다.
 *
 * 운영 환경에서는 svc 재기동 시에도 값을 잃지 않도록 RDB의
 * device_current_status 테이블과 함께 사용하는 것을 권장한다
 * (기동 시 DB에서 로드 -> 이 store에 채운 뒤 서비스 시작).
 */
@Component
public class CurrentStatusStore {

    private final Map<String, DeviceStatusDto> latestByUid = new ConcurrentHashMap<>();

    public void update(DeviceStatusDto status) {
        latestByUid.put(status.sensorNetworkUid(), status);
    }

    public List<DeviceStatusDto> snapshot() {
        return List.copyOf(latestByUid.values());
    }
}
