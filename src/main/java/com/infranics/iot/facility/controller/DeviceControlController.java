package com.infranics.iot.facility.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.infranics.iot.facility.device.DeviceControlService;
import com.infranics.iot.facility.dto.ControlRequestMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 제어는 REST로도 노출 (외부 시스템 연동, curl/Postman 테스트 용이).
 * 상태 push와 CONTROL_RESULT는 WebSocket으로 전달되므로,
 * 이 API는 "접수"만 담당하고 202 Accepted를 반환한다.
 */
@RestController
@RequestMapping("/api/devices")
@Tag(name="설비장비 제어 API", description="설비장비 제어 API")
public class DeviceControlController {

    private final DeviceControlService controlService;

    public DeviceControlController(DeviceControlService controlService) {
        this.controlService = controlService;
    }

    @PostMapping("/{sensorNetworkUid}/control")
    @Operation(summary = "설비장비 제어", description = "설비 장비를 제어 합니다.")
    public ResponseEntity<Void> control(@PathVariable String sensorNetworkUid,
                                         @Valid @RequestBody ControlRequestMessage request) {
        if (!sensorNetworkUid.equals(request.sensorNetworkUid())) {
            return ResponseEntity.badRequest().build();
        }
        controlService.handle(request);
        return ResponseEntity.accepted().build();
    }
}
