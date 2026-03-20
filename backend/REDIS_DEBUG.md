# Redis 메시지 디버깅 가이드

Resume Chat의 Redis 메시지를 실시간으로 모니터링하고 디버깅하는 방법입니다.

---

## 문제: "전송하는 메시지를 Redis에서 모니터링할 수 없다"

Redis에 저장된 메시지는 `KEYS` 명령어로 확인할 수 있지만, **실시간으로 전송되는 메시지**(Pub/Sub)는 별도 모니터링이 필요합니다.

---

## 해결 방법

### 1. 실시간 Pub/Sub 메시지 모니터링 (권장)

```bash
# 새로운 터미널 창에서 실행
./scripts/redis-pubsub-monitor.sh
```

이 스크립트는 모든 `chat:*` 채널을 구독하여 실시간 메시지를 표시합니다.

**출력 예시**:
```
=========================================
Resume Chat - Pub/Sub Message Monitor
Exit: Ctrl+C
=========================================

Subscribing to all chat:* channels...
Waiting for messages...

[2026-03-18 15:30:45] [MESSAGE]
Channel: chat:abc123
Message: {"messageId":"msg-uuid-1","sessionToken":"abc123","senderType":"APPLICANT","content":"Hello"}
---

[2026-03-18 15:30:50] [TYPING]
Channel: chat:abc123:typing
Message: {"typing":true,"userId":"user-123"}
---
```

### 2. 저장된 메시지 확인

```bash
# 저장된 메시지 확인 (스냅샷)
./scripts/redis-messages-monitor.sh
```

**출력 예시**:
```
📊 Message Statistics
Total messages in Redis: 25
Total sessions: 3

📝 Messages by Session (Recent 3 sessions)

═══════════════════════════════════════
Session: abc123
Messages: 10
═══════════════════════════════════════
Message ID: msg-uuid-1
Session: abc123
Sender: APPLICANT
Content: Hello
Created: 2026-03-18T15:30:45
Read: false
---
```

---

## Redis CLI 직접 사용

### 실시간 Pub/Sub 구독

```bash
# 모든 chat 채널 구독 (패턴 구독)
redis-cli PSUBSCRIBE "chat:*"

# 특정 세션만 구독
redis-cli SUBSCRIBE chat:abc123

# 입력 중 이벤트만 구독
redis-cli PSUBSCRIBE "chat:*:typing"

# 온라인 상태 변경만 구독
redis-cli PSUBSCRIBE "chat:*:presence"
```

**종료**: `Ctrl+C`

### 모든 Redis 명령어 모니터링

```bash
# 개발 환경에서만! (프로덕션에서는 성능 저하)
redis-cli MONITOR
```

이 명령어는 Redis 서버에서 실행되는 **모든 명령어**를 실시간으로 표시합니다.

**출력 예시**:
```
OK
1710764445.123456 [0 127.0.0.1:52345] "PUBLISH" "chat:abc123" "{...}"
1710764445.234567 [0 127.0.0.1:52346] "HSET" "chat:message:msg-uuid-1" "content" "Hello"
1710764445.345678 [0 127.0.0.1:52347] "LPUSH" "chat:messages:abc123" "msg-uuid-1"
```

---

## 메시지 흐름 확인

### 1. 메시지 전송 시 Redis 동작

```bash
# 터미널 1: Redis MONITOR 실행
redis-cli MONITOR

# 터미널 2: 애플리케이션에서 메시지 전송
# (프론트엔드 또는 API 테스트)

# 터미널 1에서 확인할 수 있는 명령어들:
# 1. HSET chat:message:{messageId} ... (메시지 저장)
# 2. EXPIRE chat:message:{messageId} 3600 (TTL 설정)
# 3. LPUSH chat:messages:{sessionToken} {messageId} (목록 추가)
# 4. LTRIM chat:messages:{sessionToken} 0 99 (최신 100개 유지)
# 5. PUBLISH chat:{sessionToken} {...} (Pub/Sub 브로드캐스트)
```

### 2. 메시지 조회 시 Redis 동작

```bash
# 터미널 1: Redis MONITOR 실행
redis-cli MONITOR

# 터미널 2: 메시지 조회 API 호출

# 터미널 1에서 확인할 수 있는 명령어들:
# 1. LRANGE chat:messages:{sessionToken} 0 -1 (메시지 ID 목록)
# 2. HGETALL chat:message:{messageId} (각 메시지 상세)
# 3. SCARD chat:unread:{sessionToken}:APPLICANT (읽지 않은 메시지 수)
```

---

## 디버깅 시나리오

### 시나리오 1: 메시지가 전송되지 않음

**1단계: Redis 연결 확인**
```bash
redis-cli ping
# 출력: PONG
```

**2단계: 실시간 Pub/Sub 모니터링**
```bash
./scripts/redis-pubsub-monitor.sh
# 또는
redis-cli PSUBSCRIBE "chat:*"
```

**3단계: 메시지 전송 후 확인**
- Pub/Sub 모니터에서 메시지가 보이는가?
  - **보임**: 백엔드 정상, 프론트엔드 WebSocket 구독 문제
  - **안 보임**: 백엔드 ChatRedisPublisher 호출 확인

**4단계: 저장된 메시지 확인**
```bash
redis-cli KEYS "chat:message:*" | wc -l
# 출력: 메시지 개수
```

### 시나리오 2: 메시지가 실시간으로 표시되지 않음

**1단계: Pub/Sub 채널 확인**
```bash
# 활성 채널 확인
redis-cli PUBSUB CHANNELS "chat:*"

# 구독자 수 확인
redis-cli PUBSUB NUMSUB chat:abc123
# 출력: "chat:abc123" "2" (2명 구독 중)
```

**2단계: Redis Subscriber 로그 확인**
```bash
# 애플리케이션 로그에서 Redis 구독 확인
tail -f logs/application.log | grep "Redis"
```

**3단계: WebSocket 연결 확인**
```bash
# 프론트엔드 콘솔에서 확인
# WebSocket 연결 상태
# STOMP 구독 상태: /topic/session/{sessionToken}
```

### 시나리오 3: 메시지가 저장되지 않음

**1단계: Redis 메모리 확인**
```bash
redis-cli INFO memory | grep used_memory_human
redis-cli INFO memory | grep maxmemory
```

**2단계: 메시지 저장 확인**
```bash
# 메시지 전송 후 즉시 확인
redis-cli KEYS "chat:message:*" | tail -5

# 최신 메시지 상세
LATEST_MSG=$(redis-cli KEYS "chat:message:*" | tail -1)
redis-cli HGETALL "$LATEST_MSG"
```

**3단계: TTL 확인**
```bash
# 메시지 만료 시간 확인 (1시간 = 3600초)
LATEST_MSG=$(redis-cli KEYS "chat:message:*" | tail -1)
redis-cli TTL "$LATEST_MSG"
# 출력: 3599 (남은 시간)
```

---

## 유용한 Redis 명령어 모음

### 메시지 관련

```bash
# 특정 세션의 메시지 ID 목록
redis-cli LRANGE chat:messages:{sessionToken} 0 -1

# 특정 메시지 상세 정보
redis-cli HGETALL chat:message:{messageId}

# 메시지 내용만 확인
redis-cli HGET chat:message:{messageId} content

# 메시지 전송 시간
redis-cli HGET chat:message:{messageId} createdAt

# 메시지 읽음 상태
redis-cli HGET chat:message:{messageId} readStatus
```

### 세션 관련

```bash
# 모든 활성 세션
redis-cli KEYS "chat:messages:*"

# 특정 세션의 메시지 수
redis-cli LLEN chat:messages:{sessionToken}

# 특정 세션의 온라인 사용자
redis-cli SMEMBERS chat:online:{sessionToken}

# 특정 세션의 읽지 않은 메시지 수
redis-cli SCARD chat:unread:{sessionToken}:APPLICANT
```

### Pub/Sub 관련

```bash
# 모든 활성 채널
redis-cli PUBSUB CHANNELS "chat:*"

# 특정 채널의 구독자 수
redis-cli PUBSUB NUMSUB chat:{sessionToken}

# 패턴 구독 수
redis-cli PUBSUB NUMPAT

# 테스트 메시지 발행
redis-cli PUBLISH chat:test '{"test":"message"}'
```

---

## 테스트 시나리오

### 전체 플로우 테스트

**터미널 1: Pub/Sub 모니터**
```bash
./scripts/redis-pubsub-monitor.sh
```

**터미널 2: Redis MONITOR**
```bash
redis-cli MONITOR
```

**터미널 3: 애플리케이션 로그**
```bash
tail -f logs/application.log
```

**터미널 4: 메시지 전송 테스트**
```bash
# API 테스트 (curl, Postman 등)
curl -X POST http://localhost:7777/api/applicant/chat/abc123/send \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"message":"Test message"}'
```

**확인 사항**:
1. 터미널 1에서 Pub/Sub 메시지 확인
2. 터미널 2에서 Redis 명령어 확인 (HSET, LPUSH, PUBLISH)
3. 터미널 3에서 애플리케이션 로그 확인
4. 프론트엔드에서 실시간 메시지 수신 확인

---

## 문제 해결 체크리스트

- [ ] Redis 서버 실행 중? (`redis-cli ping`)
- [ ] 백엔드 애플리케이션 실행 중?
- [ ] Redis 연결 설정 정확? (`application.yml`)
- [ ] Pub/Sub 채널이 활성화됨? (`PUBSUB CHANNELS`)
- [ ] 메시지가 Redis에 저장됨? (`KEYS chat:message:*`)
- [ ] Pub/Sub 메시지가 발행됨? (`redis-cli PSUBSCRIBE`)
- [ ] 프론트엔드 WebSocket 연결됨?
- [ ] 프론트엔드 STOMP 구독 설정됨?

---

## 추가 스크립트

### 모든 Redis 데이터 확인
```bash
# 모든 키 출력 (개발 환경만)
redis-cli KEYS "*"

# 키 개수
redis-cli DBSIZE

# 키 타입별 통계
for type in string list set zset hash; do
    echo "$type: $(redis-cli KEYS "*" | xargs -I {} redis-cli TYPE {} | grep $type | wc -l)"
done
```

### 특정 세션 완전 삭제
```bash
SESSION_TOKEN="abc123"

redis-cli DEL chat:messages:$SESSION_TOKEN
redis-cli DEL chat:online:$SESSION_TOKEN
redis-cli DEL chat:typing:$SESSION_TOKEN
redis-cli DEL chat:unread:${SESSION_TOKEN}:APPLICANT
redis-cli DEL chat:unread:${SESSION_TOKEN}:RECRUITER

# 해당 세션의 모든 메시지 삭제
redis-cli KEYS "chat:message:*" | while read key; do
    if [ "$(redis-cli HGET $key sessionToken)" = "$SESSION_TOKEN" ]; then
        redis-cli DEL $key
    fi
done
```

---

**문서 버전**: 1.0
**작성일**: 2026-03-18
**작성자**: Resume Chat Team
