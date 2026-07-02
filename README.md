# facility-service

7종 설비 제어 MQTT-svc-web 연동 서비스 (Spring Boot 3.3.x / Java 21 / Gradle)

`26SBP002_시공및유지관리데이터연계표준-JSON방식통신규약` 4장(장비-미들웨어 MQTT) 및
그간 논의한 svc 설계(상태-제어 토픽 비대칭, Cmd 라우팅, 요청-응답 상관관계, 정규화,
화재/통화 긴급 우선순위, WebSocket 스냅샷/제어/통화 프로토콜)를 그대로 구현했습니다.

## 빌드/실행

이 컨테이너 환경은 Maven Central 접근이 막혀 있어 `gradle build`를 직접 검증하지
못했습니다. 로컬 개발 환경(JDK 21, Gradle 8.x 설치됨)에서:

```bash
# wrapper jar가 없으므로 최초 1회 로컬 gradle로 wrapper 생성
gradle wrapper --gradle-version 8.10

./gradlew build
./gradlew bootRun
```

또는 로컬 Gradle을 그대로 사용:

```bash
gradle bootRun
```

`src/main/resources/application.yml`의 `facility.mqtt.*` 값을 실 서버/TEST 서버 정보로 맞춰주세요.
(문서 1.4절: 실서버 192.168.201.240:1883, TEST 1.212.76.242:18831, ID/PW admin/admin1234)

DB는 `spring.datasource.*`에 PostgreSQL 기준으로 잡아뒀습니다. 다른 DB를 쓰시면
`build.gradle`의 `runtimeOnly 'org.postgresql:postgresql'`을 교체하고 URL을 바꿔주세요.
(참고: 이번 코드에는 최신 상태 스냅샷을 메모리에만 보관하는 `CurrentStatusStore`만 구현했고,
재기동 시에도 유지되는 RDB 영속화 계층 - `device_current_status`, 이벤트/제어이력/통화이력 테이블 -
은 아직 붙어있지 않습니다. `datasource` 설정은 이후 이 영속화 계층을 붙일 때 바로 쓰도록 미리 둔 것입니다.)

## 패키지 구조와 설계 매핑

```
config/       MqttProperties, FacilityCommandProperties, FacilityCallProperties, WebSocketConfig, JacksonConfig
mqtt/         FacilityTopic(토픽 파싱/생성), MqttGateway(Paho 연결/구독/발행),
              MqttMessageReceivedEvent, DeviceMessageDispatcher(라우팅 총괄)
device/       DeviceType(7종 설비 enum), DeviceControlService(web->MQTT 제어 발행)
correlation/  PendingCommandTracker(요청-응답 상관관계 + 타임아웃)
normalize/    DeviceStatusDto, DeviceStatusNormalizer(인터페이스),
              DefaultDeviceStatusNormalizer(fallback), PumpStatusNormalizer, FireStatusNormalizer,
              DeviceStatusNormalizerRegistry, CurrentStatusStore(최신상태 인메모리)
call/         CallSession, CallSessionState, CallSessionManager(통화 상태머신),
              CallEventPriorityDispatcher
ws/           DeviceWebSocketHandler(/ws/devices), DeviceWebSocketBroadcaster
controller/   DeviceControlController(REST 제어), CallController(REST 통화응답)
dto/          WebSocket 인/아웃바운드 메시지 (STATUS_UPDATE, INITIAL_SNAPSHOT, CONTROL_RESULT,
              CALL_INCOMING/QUEUED/TIMEOUT/ACCEPTED/ENDED, CONTROL_REQUEST, CALL_RESPONSE)
```

## MqttGateway <-> CallSessionManager 순환 의존 회피

MqttGateway가 CallSessionManager를 직접 알면 순환 의존(Mqtt -> Dispatcher -> Call -> Mqtt)이
생기므로, MqttGateway는 메시지 수신 시 `MqttMessageReceivedEvent`만 발행하고
`DeviceMessageDispatcher`가 `@EventListener`로 받아 정규화/브로드캐스트/통화 라우팅을 담당합니다.
CallSessionManager는 제어 명령 발행을 위해 MqttGateway에 단방향으로만 의존합니다.

## WebSocket 프로토콜

엔드포인트: `ws://localhost:8080/ws/devices`

접속 직후 svc -> web: `INITIAL_SNAPSHOT` (현재 보관 중인 모든 장비 최신 상태)

이후 svc -> web (일반):
```json
{ "type": "STATUS_UPDATE", "deviceType": "PUMP", "sensorNetworkUid": "...", "timestamp": "...", "fields": { ... } }
```

화재/통화 요청 등 긴급 이벤트는 `broadcastImmediate()`로 별도 표시 (현재는 동기 즉시발송으로 동일 구현,
추후 일반 상태에 큐/배치 도입 시에도 이 경로는 우회하도록 미리 분리해둠).

web -> svc 제어:
```json
{ "type": "CONTROL_REQUEST", "requestId": "uuid", "deviceType": "PUMP",
  "sensorNetworkUid": "0001-01-01-01", "cmd": "0x01", "data": "0x01",
  "expectedResponseCmd": null }
```

REST로도 동일 기능 제공: `POST /api/devices/{sensorNetworkUid}/control` (202 Accepted, 결과는 WebSocket으로 push)

통화 응답:
```json
{ "type": "CALL_RESPONSE", "callId": "...", "action": "ACCEPT" }
```
REST: `POST /api/calls/{callId}/response`

## 아직 채워 넣지 않은 부분 (설계 논의에서 나온 TODO)

1. **RDB 영속화**: 최신상태/이벤트로그/제어명령이력/통화이력 테이블 및 JPA 또는 JdbcTemplate 리포지토리.
   `CallSessionManager.endSession()`, `DeviceMessageDispatcher` 등에 TODO 주석으로 표시해둔 지점부터 시작하면 됩니다.
2. **인증/권한**: WebSocket 연결 시 JWT 검증, 메시지별 권한 재검증 로직 없음. `DeviceWebSocketHandler`에 훅 필요.
3. **재기동 시 상태 복구**: `CurrentStatusStore`가 현재 인메모리뿐 -> 기동 시 DB에서 로드하는 초기화 로직 필요.
4. **다중 인스턴스 확장**: 지금은 단일 인스턴스 기준. svc를 여러 대로 늘리면 Redis Pub/Sub 등으로
   인스턴스 간 브로드캐스트 동기화가 필요합니다.
5. **LWT(장비 오프라인 감지)**: MQTT Last Will 설정 및 오프라인 상태를 web에 알리는 로직 미포함.
6. **나머지 장비 전용 Normalizer**: Light/Louver/Access/Interphone은 현재 `DefaultDeviceStatusNormalizer`
   (Data 필드를 그대로 Map화)로 처리됩니다. `PumpStatusNormalizer`, `FireStatusNormalizer`를 참고해서
   실제 연동 테스트하며 채워나가시면 됩니다.
7. **7번째 설비명 미확인**: 문서 표에 6종만 명시됨.
