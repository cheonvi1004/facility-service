package com.yourorg.facility.correlation;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * web -> svc -> 장비 로 나간 제어 명령이, 장비의 응답 Cmd로 확인되는지 추적한다.
 * 문서상 요청/응답 Cmd 쌍 예: Light 0xA3 -> 0x3A, Fire 0x01 -> 0x61
 */
@Component
public class PendingCommandTracker {

    private record Key(String sensorNetworkUid, String requestCmd) {
    }

    public record PendingCommand(
            String requestId,
            String sensorNetworkUid,
            String requestCmd,
            String expectedResponseCmd,
            Instant sentAt
    ) {
    }

    private final Map<Key, PendingCommand> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
            r -> {
                Thread t = new Thread(r, "pending-cmd-timeout");
                t.setDaemon(true);
                return t;
            });

    public void track(String requestId, String sensorNetworkUid, String requestCmd, String expectedResponseCmd,
                       Duration timeout, Consumer<PendingCommand> onTimeout) {
        Key key = new Key(sensorNetworkUid, requestCmd);
        PendingCommand cmd = new PendingCommand(requestId, sensorNetworkUid, requestCmd, expectedResponseCmd, Instant.now());
        pending.put(key, cmd);

        scheduler.schedule(() -> {
            PendingCommand still = pending.remove(key);
            if (still != null) {
                onTimeout.accept(still);
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 장비로부터 응답 Cmd 수신 시 호출. 매칭되는 pending이 있으면 제거하고 반환한다.
     */
    public Optional<PendingCommand> resolve(String sensorNetworkUid, String responseCmd) {
        return pending.entrySet().stream()
                .filter(e -> e.getKey().sensorNetworkUid().equals(sensorNetworkUid)
                        && e.getValue().expectedResponseCmd().equalsIgnoreCase(responseCmd))
                .findFirst()
                .map(e -> {
                    pending.remove(e.getKey());
                    return e.getValue();
                });
    }

    public Map<String, PendingCommand> snapshotByRequestId() {
        return pending.values().stream()
                .collect(Collectors.toMap(PendingCommand::requestId, p -> p));
    }
}
