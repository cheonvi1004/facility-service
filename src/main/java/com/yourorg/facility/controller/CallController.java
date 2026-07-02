package com.yourorg.facility.controller;

import com.yourorg.facility.call.CallSessionManager;
import com.yourorg.facility.dto.CallResponseMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calls")
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
