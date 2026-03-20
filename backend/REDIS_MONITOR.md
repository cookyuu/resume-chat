# Redis 모니터링 가이드

Resume Chat의 Redis 채팅 시스템을 모니터링하는 방법을 안내합니다.

---

## 1. 기본 명령어

### Redis 서버 상태 확인

```bash
# Redis 연결 확인
redis-cli ping
# 출력: PONG

# 서버 정보
redis-cli INFO

# 메모리 사용량
redis-cli INFO memory | grep used_memory_human

# 연결된 클라이언트 수
redis-cli INFO clients | grep connected_clients
```

### 키 조회

```bash
# 모든 채팅 메시지 키 확인
redis-cli KEYS "chat:message:*"

# 모든 세션 키 확인
redis-cli KEYS "chat:messages:*"

# 온라인 사용자 확인
redis-cli KEYS "chat:online:*"

# 특정 키의 타입 확인
redis-cli TYPE chat:message:123

# 특정 키의 TTL 확인 (만료 시간)
redis-cli TTL chat:message:123
```

---

## 2. 모니터링 스크립트 사용

### 2.1 일반 모니터링

```bash
# scripts 디렉토리에서 실행
./scripts/redis-monitor.sh
```

**출력 내용**:
- Redis 연결 상태
- 메모리 사용량
- 연결된 클라이언트 수
- 채팅 메시지 통계
- 최근 활동 세션
- Pub/Sub 채널 상태
- 키 만료 시간
- 느린 쿼리

### 2.2 실시간 모니터링

```bash
# 1초마다 갱신
./scripts/redis-realtime.sh

# 종료: Ctrl+C
```

**출력 내용**:
- 실시간 메모리 사용량
- 연결된 클라이언트 수
- 채팅 통계 (메시지, 세션, 온라인 사용자)
- 초당 명령어 수
- 활성 Pub/Sub 채널
- 최근 활동 세션

---

## 3. 채팅 시스템 특화 명령어

### 세션 모니터링

```bash
# 특정 세션의 메시지 수 확인
redis-cli LLEN chat:messages:{sessionToken}

# 특정 세션의 최근 메시지 10개 확인
redis-cli LRANGE chat:messages:{sessionToken} 0 9

# 특정 세션의 온라인 사용자 확인
redis-cli SMEMBERS chat:online:{sessionToken}
```

### 메시지 상세 확인

```bash
# 특정 메시지 상세 정보
redis-cli HGETALL chat:message:{messageId}

# 메시지 필드 개별 조회
redis-cli HGET chat:message:{messageId} content
redis-cli HGET chat:message:{messageId} senderType
```

### 읽지 않은 메시지 확인

```bash
# 특정 세션의 읽지 않은 메시지 (지원자 기준)
redis-cli SMEMBERS chat:unread:{sessionToken}:APPLICANT

# 특정 세션의 읽지 않은 메시지 (채용담당자 기준)
redis-cli SMEMBERS chat:unread:{sessionToken}:RECRUITER

# 읽지 않은 메시지 수
redis-cli SCARD chat:unread:{sessionToken}:APPLICANT
```

### 입력 중 상태 확인

```bash
# 특정 세션의 입력 중 상태
redis-cli HGETALL chat:typing:{sessionToken}
```

---

## 4. Pub/Sub 모니터링

### 채널 확인

```bash
# 모든 chat 관련 채널 확인
redis-cli PUBSUB CHANNELS "chat:*"

# 특정 채널의 구독자 수
redis-cli PUBSUB NUMSUB chat:{sessionToken}

# 패턴 구독 수
redis-cli PUBSUB NUMPAT
```

### 실시간 메시지 확인

```bash
# 모든 Redis 명령어 실시간 확인 (개발 환경만!)
redis-cli MONITOR

# 특정 채널 구독 (테스트용)
redis-cli SUBSCRIBE chat:{sessionToken}

# 패턴 구독
redis-cli PSUBSCRIBE "chat:*"
```

---

## 5. 성능 모니터링

### 느린 쿼리 확인

```bash
# 최근 10개 느린 쿼리
redis-cli SLOWLOG GET 10

# 느린 쿼리 개수
redis-cli SLOWLOG LEN

# 느린 쿼리 초기화
redis-cli SLOWLOG RESET
```

### 명령어 통계

```bash
# 총 명령어 수
redis-cli INFO stats | grep total_commands_processed

# 초당 명령어 수
redis-cli INFO stats | grep instantaneous_ops_per_sec

# 키스페이스 히트/미스 비율
redis-cli INFO stats | grep keyspace_hits
redis-cli INFO stats | grep keyspace_misses
```

### 메모리 분석

```bash
# 메모리 상세 정보
redis-cli INFO memory

# 메모리 사용량 상위 키 (redis-cli 4.0+)
redis-cli --bigkeys

# 메모리 사용량 분석
redis-cli MEMORY USAGE chat:message:{messageId}
```

---

## 6. 데이터 정리

### 수동 정리

```bash
# 특정 패턴의 키 삭제
redis-cli KEYS "chat:message:*" | xargs redis-cli DEL

# 만료된 키 강제 삭제
redis-cli --scan --pattern "chat:*" | while read key; do
    ttl=$(redis-cli TTL $key)
    if [ "$ttl" -eq -2 ]; then
        redis-cli DEL $key
    fi
done

# 특정 세션 데이터 삭제
redis-cli DEL chat:messages:{sessionToken}
redis-cli DEL chat:online:{sessionToken}
redis-cli DEL chat:typing:{sessionToken}
```

### 전체 초기화 (주의!)

```bash
# 모든 키 삭제
redis-cli FLUSHALL

# 현재 DB만 삭제
redis-cli FLUSHDB
```

---

## 7. 프로덕션 모니터링 도구

### Redis Commander (GUI)

```bash
# 설치
npm install -g redis-commander

# 실행
redis-commander

# 브라우저: http://localhost:8081
```

### RedisInsight (공식 GUI)

- 다운로드: https://redis.io/insight/
- 기능: 키 브라우징, 메모리 분석, 성능 모니터링

### Prometheus + Grafana

**redis_exporter 설치**:
```bash
docker run -d --name redis_exporter \
  -p 9121:9121 \
  oliver006/redis_exporter \
  --redis.addr=redis://localhost:6379
```

**Prometheus 설정** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']
```

**Grafana 대시보드**:
- Import ID: 763 (Redis Dashboard for Prometheus)

---

## 8. 알람 설정

### 메모리 사용량 알람

```bash
# 스크립트로 모니터링
while true; do
    MEMORY=$(redis-cli INFO memory | grep used_memory_human | cut -d: -f2 | sed 's/M//')
    if (( $(echo "$MEMORY > 500" | bc -l) )); then
        echo "경고: Redis 메모리 사용량이 500MB를 초과했습니다!"
        # 알림 전송 (예: Slack, 이메일)
    fi
    sleep 60
done
```

### 연결 수 알람

```bash
# 연결 수 모니터링
while true; do
    CLIENTS=$(redis-cli INFO clients | grep connected_clients | cut -d: -f2)
    if [ "$CLIENTS" -gt 100 ]; then
        echo "경고: 연결된 클라이언트가 100개를 초과했습니다!"
    fi
    sleep 60
done
```

---

## 9. 문제 해결

### Redis 응답 느림

```bash
# 1. 느린 쿼리 확인
redis-cli SLOWLOG GET 10

# 2. 메모리 사용량 확인
redis-cli INFO memory

# 3. 연결 수 확인
redis-cli CLIENT LIST

# 4. 키스페이스 분석
redis-cli --bigkeys
```

### 메모리 부족

```bash
# 1. 메모리 사용량 확인
redis-cli INFO memory | grep used_memory_human

# 2. 가장 큰 키 찾기
redis-cli --bigkeys

# 3. 만료 시간 설정 확인
redis-cli KEYS "chat:*" | head -10 | while read key; do
    redis-cli TTL $key
done

# 4. LRU eviction 설정 (redis.conf)
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### Pub/Sub 메시지 유실

```bash
# 1. 구독자 확인
redis-cli PUBSUB NUMSUB chat:{sessionToken}

# 2. 연결 상태 확인
redis-cli CLIENT LIST | grep sub

# 3. 로그 확인
tail -f /var/log/redis/redis-server.log
```

---

## 10. 모니터링 체크리스트

### 일일 체크

- [ ] 메모리 사용량 확인
- [ ] 연결된 클라이언트 수 확인
- [ ] 키 개수 확인
- [ ] 느린 쿼리 확인

### 주간 체크

- [ ] 메모리 증가 추세 분석
- [ ] Hit/Miss 비율 확인
- [ ] TTL 설정 검토
- [ ] 불필요한 키 정리

### 월간 체크

- [ ] 전체 성능 리뷰
- [ ] 용량 계획 수립
- [ ] 백업 전략 검토
- [ ] Redis 버전 업데이트 검토

---

**문서 버전**: 1.0
**작성일**: 2026-03-18
**작성자**: Resume Chat Team
