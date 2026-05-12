# 음성 주문 STT 빈 결과 — 진단 명령어 정리

증상: 사용자가 음성 주문 시도했는데 `engine_used` null + `whisper_text/google_text` 빈값으로 저장됨. STT 가 throw 한 게 아니라 정상 응답에 빈 텍스트만 들어옴.

아래는 원인 좁힐 때 실제 사용한 명령어 모음.

---

## 1. Docker 로그 — 컨테이너 가동 상태 + voice-order 활동

```bash
# 컨테이너 시작 시각 (이 이전 로그는 사라짐)
docker inspect cheonil-server --format '{{.State.StartedAt}}'

# 최근 voice-order 관련 로그만
docker logs cheonil-server --since 60m 2>&1 | grep -E "INFO.*VoiceOrder"

# 에러/경고만 (Hibernate 등 노이즈 제외)
docker logs cheonil-server --since 30m 2>&1 \
  | grep -E "ERROR|WARN" \
  | grep -vE "Hibernate|Tomcat|SpringDoc|HHH"
```

**얻은 정보**: 컨테이너 시작 시각과 DB row 시각 비교로 "재기동 이후 로그가 사라진 것" vs "실제로 안 찍힌 것" 구분.

---

## 2. DB — 로그 row 패턴 검사

```bash
# 최근 5건 요약
docker exec cheonil-db psql -U root -d cheonil -c "
  SELECT seq, engine_used,
         left(coalesce(whisper_text,'(null)'),50) AS whisper,
         left(coalesce(google_text,'(null)'),50) AS google,
         order_seq, created_at
  FROM t_voice_order_log
  ORDER BY created_at DESC LIMIT 5;
"

# 의심 row 의 전체 컬럼
docker exec cheonil-db psql -U root -d cheonil -c "
  SELECT seq, engine_used, whisper_text, google_text,
         final_text, error_message, audio_size_bytes, audio_mime
  FROM t_voice_order_log WHERE seq IN (5, 8, 9);
"

# NULL vs 빈 문자열 구분 — 둘이 표시상 같아 보임
docker exec cheonil-db psql -U root -d cheonil -c "
  SELECT seq,
         whisper_text IS NULL AS w_null,
         char_length(coalesce(whisper_text,'')) AS w_len,
         google_text IS NULL AS g_null,
         char_length(coalesce(google_text,'')) AS g_len
  FROM t_voice_order_log WHERE seq IN (5,8,9);
"
```

**얻은 정보**: `is null = false / length = 0` → STT 가 빈 문자열(`""`)을 정상 응답으로 반환했음을 확인.

---

## 3. 파일 시스템 — audio 파일 존재/사이즈

```bash
docker exec cheonil-server ls -la /data/voice-orders/2026/05/12/

# DB 의 audio_path 로 정확한 파일 조회
docker exec cheonil-db psql -U root -d cheonil -t -c \
  "SELECT audio_path FROM t_voice_order_log WHERE seq=9;"
```

---

## 4. 오디오 파일 분석 (호스트로 꺼내서)

```bash
# 컨테이너에서 호스트로 복사
docker cp cheonil-server:/data/voice-orders/2026/05/12/<uuid>.mp3 /tmp/seq9.mp3

# 1차 진단 — file 명령 (헤더 추측, 부정확할 수 있음)
file /tmp/seq9.mp3
# → "ISO Media, MP4 v2 [ISO 14496-14]"  (이건 잘못된 추측이었음)

# macOS afinfo — 정확한 메타
afinfo /tmp/seq9.mp3
# → File type ID: MPG3
#    Data format: 2 ch, 44100 Hz, .mp1   ← Layer 1 (구형!)
#    audio packets: 1                    ← 5초 분량이 1패킷 = 비정상

# 실제 재생 (간단히 들어보기)
afplay /tmp/seq9.mp3
```

**얻은 정보**: 헤더상 mp1 (MPEG Audio Layer 1, mp3 와 다른 구형 포맷), packet 수 비정상 → 디코더 호환성 문제 의심.

---

## 5. STT 직접 호출 — 우리 Spring 코드 우회

원인이 우리 코드인지 STT 자체인지 가르는 핵심.

```bash
# Whisper 컨테이너 헬스
curl -s -o /dev/null -w "Whisper: %{http_code} (%{time_total}s)\n" \
  --max-time 5 http://localhost:9000/docs

# 같은 mp3 를 Whisper 에 직접 (audio/mpeg)
docker exec cheonil-server curl -s -X POST \
  "http://whisper:9000/asr?language=ko&task=transcribe&output=json" \
  -F "audio_file=@/data/voice-orders/2026/05/12/<uuid>.mp3;type=audio/mpeg" \
  -w "\nHTTP %{http_code}\n"

# MIME 만 audio/mp4 로 바꿔서 재시도 (Whisper 가 디코더 선택할 hint)
docker exec cheonil-server curl -s -X POST \
  "http://whisper:9000/asr?language=ko&task=transcribe&output=json" \
  -F "audio_file=@/data/voice-orders/.../<uuid>.mp3;type=audio/mp4"

# 확장자도 m4a 로
docker exec cheonil-server sh -c '
  cp /data/voice-orders/.../<uuid>.mp3 /tmp/test.m4a
  curl -s -X POST "http://whisper:9000/asr?language=ko&task=transcribe&output=json" \
    -F "audio_file=@/tmp/test.m4a;type=audio/mp4"
'
```

**얻은 정보**: 다양한 MIME/확장자 다 빈 결과 → Whisper 가 이 파일 디코딩 실패 확인. 우리 코드 무관.

---

## 6. ffmpeg 로 변환 + 재시도 — 결정타

```bash
# Whisper 컨테이너에 ffmpeg 있으니 활용
docker cp /tmp/seq9.mp3 cheonil-whisper:/tmp/seq9.mp3

docker exec cheonil-whisper sh -c '
  # ffmpeg 가 파일 형식 분석 (file 명령보다 정확)
  ffmpeg -i /tmp/seq9.mp3 2>&1 | head -15

  # WAV 16kHz mono 로 강제 변환
  ffmpeg -y -i /tmp/seq9.mp3 -ar 16000 -ac 1 /tmp/seq9.wav

  # 변환된 WAV 로 Whisper 재호출
  curl -s -X POST "http://localhost:9000/asr?language=ko&task=transcribe&output=json" \
    -F "audio_file=@/tmp/seq9.wav;type=audio/wav"
'
```

**얻은 결과**:
- ffmpeg 분석 → `Input #0, mov,mp4,m4a,3gp,3g2,mj2` + metadata `com.android.version: 16` (실제 컨테이너는 m4a/AAC, Android 16 앱 산물)
- WAV 변환 → Whisper 정상 인식 `"효성 창성, 뚝배기 불고기 2개"`

→ **원인 확정**: MIME 메타데이터(`audio/mpeg`)와 실제 컨테이너(m4a/AAC) mismatch. 데이터 자체는 멀쩡함.

---

## 한 줄 요약 흐름

```
"STT 가 빈 결과 준다"
  ↓ DB 검사
"실제로 빈 문자열 반환됨 (exception 아님)"
  ↓ Docker logs / 컨테이너 시작시각
"로그 안 찍히는 게 정상 (예외 안 났음)"
  ↓ audio 파일 직접 분석 (afinfo / ffmpeg)
"컨테이너는 m4a 인데 MIME 은 mpeg"
  ↓ WAV 로 변환 후 재시도
"정상 인식됨 → 파일 자체는 OK, 디코더가 헷갈렸을 뿐"
```

---

## 적용한 해결책

1. **백엔드 — AudioNormalizer** ([`src/main/java/com/ban/cheonil/speech/AudioNormalizer.java`](../../src/main/java/com/ban/cheonil/speech/AudioNormalizer.java))
   - ffmpeg subprocess 로 STT 호출 전 WAV 16kHz mono 정규화
   - Dockerfile 에 `ffmpeg` 패키지 추가
2. **모바일 — MIME 명시** (`cheonil-app/src/apis/voiceOrderApi.ts`)
   - `new File(uri)` 의 type URI 추론 회피 → `arrayBuffer()` 로 읽어 명시적 type 의 Blob 으로 wrap
   - type: `audio/m4a` → `audio/mp4` (IANA 표준)

---

## 일반화 가능한 진단 순서

1. **로그 먼저** — 컨테이너 lifecycle 안에 있나 (재기동으로 사라졌는지)
2. **DB 상태 검사** — null vs 빈문자열 명확히 구분 (`char_length`)
3. **호스트로 산물 꺼내기** — `docker cp` 로 audio 확보
4. **메타데이터 다중 확인** — `file` / `afinfo` / `ffmpeg -i` 셋 다 (한 명령만 믿지 말 것)
5. **외부 의존 서비스 직접 호출** — 우리 코드 vs 외부 서비스 책임 가르기
6. **ffmpeg 로 표준 포맷 변환 후 재시도** — 데이터 자체 손상 vs 포맷 호환 문제 구분
