package com.infranics.iot.facility.normalize;


import com.infranics.iot.facility.entity.DeviceCurrentStatus;
import com.infranics.iot.facility.repository.DeviceCurrentStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeviceStatusInitializer {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusInitializer.class);

    private final DeviceCurrentStatusRepository currentStatusRepository;
    private final CurrentStatusStore statusStore;

    public DeviceStatusInitializer(DeviceCurrentStatusRepository currentStatusRepository, 
                                   CurrentStatusStore statusStore) {
        this.currentStatusRepository = currentStatusRepository;
        this.statusStore = statusStore;
    }

    /**
     * Spring Boot 애플리케이션 기동이 완전히 끝나면 자동으로 1회 실행됩니다.
     * DB의 device_current_status 테이블에서 전체 데이터를 읽어와 메모리에 캐싱합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadInitialData() {
        log.info("RDB로부터 장비 최신 상태 스냅샷 로딩을 시작합니다...");
        
        List<DeviceCurrentStatus> entities = currentStatusRepository.findAll();
        int loadCount = 0;
        
        for (DeviceCurrentStatus entity : entities) {
            // DB Entity -> 메모리 관리용 DTO로 변환
            DeviceStatusDto dto = new DeviceStatusDto(
                    entity.getApiKey(),
                    entity.getSensorNetworkUid(),
                    entity.getDeviceType(),
                    entity.getCmd(),
                    entity.getReceivedAt().toInstant(),
                    entity.getNormalizedFields(),
                    entity.getRawPayload()
            );
            
            // CurrentStatusStore 메모리에 적재
            statusStore.update(dto);
            loadCount++;
        }
        
        log.info("장비 최신 상태 {}건 로딩 완료.", loadCount);
    }
}