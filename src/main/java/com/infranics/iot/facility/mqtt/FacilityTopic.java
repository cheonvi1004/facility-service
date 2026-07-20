package com.infranics.iot.facility.mqtt;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.infranics.iot.facility.device.DeviceType;

/**
 * 문서 4.1절 토픽 규약.
 * 상태(Status)와 제어(Ctrl) 토픽의 depth가 다르므로 별도 처리한다.
 *
 * 장비 -> svc : device/[ApiKey]/[장비종류]/Status
 * svc -> 장비 : device/[ApiKey]/[장비종류]/[SensorNetworkUID]/Ctrl
 */
public final class FacilityTopic {

    private static final Pattern STATUS_TOPIC =
            Pattern.compile("^device/([^/]+)/([^/]+)/Status$");

    private FacilityTopic() {
    }

    public static String statusSubscribeFilter() {
        return "device/+/+/Status";
    }

    public static String ctrlPublishTopic(String apiKey, DeviceType type, String sensorNetworkUid) {
        return "device/%s/%s/%s/Ctrl".formatted(apiKey, type.topicName(), sensorNetworkUid);
    }

    public static Optional<StatusTopicParts> parseStatus(String topic) {
        Matcher m = STATUS_TOPIC.matcher(topic);
        if (!m.matches()) {
            return Optional.empty();
        }
        DeviceType type;
        try {
            type = DeviceType.fromTopicName(m.group(2));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.of(new StatusTopicParts(m.group(1), type));
    }

    public record StatusTopicParts(String apiKey, DeviceType deviceType) {
    }
}
