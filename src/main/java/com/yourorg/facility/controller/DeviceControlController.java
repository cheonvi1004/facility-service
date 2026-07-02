package com.yourorg.facility.controller;

import com.yourorg.facility.device.DeviceControlService;
import com.yourorg.facility.dto.ControlRequestMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 제어는 REST로도 노출 (외부 시스템 연동, curl/Postman 테스트 용이).
 * 상태 push와 CONTROL_RESULT는 WebSocket으로 전달되므로,
 * 이 API는 "접수"만 담당하고 202 Accepted를 반환한다.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceControlController {

    private final DeviceControlService controlService;

    public DeviceControlController(DeviceControlService controlService) {
        this.controlService = controlService;
    }

    @PostMapping("/{sensorNetworkUid}/control")
    public ResponseEntity<Void> control(@PathVariable String sensorNetworkUid,
                                         @Valid @RequestBody ControlRequestMessage request) {
        if (!sensorNetworkUid.equals(request.sensorNetworkUid())) {
            return ResponseEntity.badRequest().build();
        }
        controlService.handle(request);
        return ResponseEntity.accepted().build();
    }
}
