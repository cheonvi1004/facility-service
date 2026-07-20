package com.infranics.iot.facility.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnectionTestv3 {

    public static void main(String[] args) {

        String broker   = "tcp://1.212.76.242:18831";
        String clientId = "facility-svc-1";   //publisher와 clientId 중복 금지
        String username = "admin";           //← 실제 username으로 변경
        String password = "admin1234";           //← 실제 password로 변경
        String topic    = "device/+/+/Status";    //← 실제 구독 토픽

        try {

             //null 대신 MemoryPersistence 명시 → 브로커에 CONNECT 패킷 정상 전달
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);  //테스트 소스: 재연결 비활성화
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(10);
            options.setUserName(username);
            options.setPassword(password.toCharArray());

             //callback 등록
            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("[수신] topic=" + topic + " payload=" + new String(message.getPayload()));
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("MQTT 연결 끊김: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

             //연결
            client.connect(options);
            System.out.println("MQTT 연결 성공: " + broker);

             //subscribe (QoS 0 - 테스트용)
            client.subscribe(topic, 0);
            System.out.println("구독 완료: " + topic);

             //30초간 메시지 수신 대기
            Thread.sleep(30000);

             //안전한 종료
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            System.out.println("MQTT 연결 종료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

