package com.infranics.iot.facility.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.infranics.iot.facility.call.CallSessionManager;
import com.infranics.iot.facility.dto.CallResponseMessage;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/calls")
@Tag(name="Call", description="Call")
public class CallController {

    private final CallSessionManager callSessionManager;

    public CallController(CallSessionManager callSessionManager) {
        this.callSessionManager = callSessionManager;
    }

    @PostMapping("/{callId}/response")
    public ResponseEntity<Void> respond(@PathVariable String callId, @Valid @RequestBody CallResponseMessage request) {
        if (!callId.equals(request.callId())) {
            return ResponseEntity.badRequest().build();
        }
        callSessionManager.onOperatorResponse(callId, request.action() == CallResponseMessage.Action.ACCEPT);
        return ResponseEntity.accepted().build();
    }
}
