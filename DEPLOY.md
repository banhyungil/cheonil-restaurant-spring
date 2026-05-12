# 배포 가이드

매장 운영 PC (Windows) 에 천일 시스템을 처음 올리고 운영하는 절차.

---

## 1. 사전 조건

| 항목 | 비고 |
|-----|-----|
| Docker Desktop | Windows / Mac 둘 다 동일 |
| git | 저장소 동기화용 |
| 인터넷 연결 | Cloudflare Tunnel + claude CLI 가 외부 통신 필요 |
| 디렉터리 구조 | `cheonil-restaurant-spring` 과 `cheonil-restaurant-next` 가 sibling 폴더 |

```
<ROOT>/
├── cheonil-restaurant-spring/   ← 본 디렉터리 (백엔드 + docker-compose)
└── cheonil-restaurant-next/     ← 프론트엔드 (web 컨테이너에서 빌드)
```

---

## 2. 최초 배포 — 수동 단계 (한 번만)

### 2-1. 저장소 clone

```bash
git clone <backend-repo> cheonil-restaurant-spring
git clone <frontend-repo> cheonil-restaurant-next
cd cheonil-restaurant-spring
```

### 2-2. `.env` 작성

```bash
cp .env.example .env
```

`.env` 편집:

```bash
# Cloudflare Zero Trust > Networks > Connectors 에서 발급한 터널 토큰
CLOUDFLARED_TOKEN=eyJhIjoi...

# Google Cloud Console 에서 TTS API 활성화 후 발급 받은 키 (TTS 사용 시)
GOOGLE_TTS_API_KEY=AIza...
```

> ⚠️ `.env` 는 git 에 커밋 X (`.gitignore` 에 명시돼있음). 따로 안전한 곳에 백업.

### 2-3. 첫 docker compose 기동

```bash
docker compose up -d --build
```

- 첫 빌드는 시간 걸림 (jar 빌드 + claude installer + Whisper 이미지 + MeloTTS 모델 다운로드 등)
- 백그라운드 (`-d`) 로 떠있는지 확인:
  ```bash
  docker compose ps
  ```
  모든 서비스가 `Up` 또는 `Up (healthy)` 이어야.

### 2-4. claude CLI 로그인 (interactive, 한 번만)

```bash
docker compose exec app claude /login
```

- 화면에 URL + 코드 표시
- 다른 PC/폰 브라우저에서 URL 열고 Anthropic 로그인 → 코드 입력
- 컨테이너의 claude 가 OAuth 토큰 받아 `/root/.claude` (v-claude volume) 에 저장
- 이후 컨테이너 재시작/재빌드 시에도 영구 유지

확인:
```bash
docker compose exec app sh -c 'echo "안녕 한국어로" | claude -p'
```
한국어 응답 떠야 OK.

### 2-5. 동작 검증

| 항목 | 확인 명령 / URL |
|-----|---------------|
| Spring API (로컬) | `curl http://localhost:8080/api/menus` |
| Cloudflare Tunnel | `curl https://api.cheonil.org/api/menus` |
| 매장 관리 웹 | 브라우저로 `http://localhost` |
| Whisper STT | `docker compose logs whisper` (모델 로드 후 Ready) |
| MeloTTS | `docker compose logs melotts` |

모두 정상 응답하면 운영 가능.

---

## 3. 운영 — 일상 배포

### 코드 업데이트 + 재배포 (한 번에)

```bash
# Windows
scripts\deploy.bat

# macOS / Linux (예정 — 필요 시 .sh 추가)
```

`deploy.bat` 가 처리:
1. Frontend git pull
2. Backend git pull
3. `docker compose build`
4. `docker compose up -d`

`.env` 와 claude 로그인은 그대로 유지 (volume 영속).

### 부분 재시작

```bash
docker compose restart app           # Spring 만
docker compose restart cloudflared   # 터널만 (DNS resolve 문제 시)
docker compose up -d --build app     # Spring 이미지 재빌드
```

### 로그 확인

```bash
docker compose logs -f app             # Spring 실시간
docker compose logs -f cloudflared     # 터널 실시간
docker compose logs --tail 100 app     # 마지막 100줄
```

---

## 4. DB 백업

```bash
# Windows
scripts\db-backup.bat

# macOS / Linux
./scripts/db-backup.sh
```

- `backup/cheonil_YYYYMMDD_HHMMSS.dump` (전체 복원용)
- `backup/cheonil_YYYYMMDD_HHMMSS_data.sql` (data-only)
- `RETENTION_DAYS` (기본 14) 일 초과 파일 자동 삭제

스케줄링: Windows 작업 스케줄러 / cron 으로 매일 1회 권장.

---

## 5. 토큰/세션 만료 대응

| 항목 | 만료 | 갱신 방법 |
|-----|-----|---------|
| Cloudflare Tunnel 토큰 | 거의 영구 (인스턴스 삭제 시까지) | Zero Trust 대시보드에서 새 토큰 → `.env` 교체 → `docker compose restart cloudflared` |
| Let's Encrypt cert (mqtt.cheonil.org) | 90일 | Lightsail 의 certbot.timer 자동 갱신 (별도 작업 불필요) |
| claude OAuth 토큰 | 수개월 | `docker compose exec app claude /login` 재실행 |
| Google TTS API 키 | 영구 | (필요 시 Console 에서 rotate) |

---

## 6. 트러블슈팅

### `Not logged in · Please run /login` (claude 호출 시)

```bash
docker compose exec app claude /login
```

### Cloudflare Tunnel 502 (DNS resolve 실패)

`app` 컨테이너 시작 후 cloudflared 가 DNS 캐시 실패 상태일 수 있음:
```bash
docker compose restart cloudflared
```

### 디스크 가득 참

```bash
docker system prune -a          # 안 쓰는 이미지/컨테이너 정리
docker volume ls                 # volume 확인
```

`v-whisper-models`, `v-melotts-models` 가 큼 (각 1GB+).

### Spring 시작 실패

```bash
docker compose logs app --tail 50
```

흔한 원인:
- DB 연결 실패 → `db` 컨테이너 healthy 인지 확인
- 포트 충돌 → 8080 이 다른 프로세스에 잡힘

### 백업 / 복원

```bash
# 백업
./scripts/db-backup.sh

# 전체 복원 (custom dump 사용)
scripts\db-restore-full.bat  cheonil_YYYYMMDD_HHMMSS.dump

# 데이터만 append
scripts\db-restore-append.bat  cheonil_YYYYMMDD_HHMMSS_data.sql
```

---

## 7. 다른 환경 (dev Mac) 셋업 차이

- `.env` 의 `CLOUDFLARED_TOKEN` 은 dev 환경엔 불필요 — 빼고 가능 (해당 컨테이너 stop)
- claude 로그인은 환경별로 별도 — Mac 의 컨테이너와 Windows 의 컨테이너는 독립
- DB volume 도 별도 — 데이터 공유 안 됨

---

## 요약 — 수동 단계는 이것뿐

1. `.env` 작성 (`CLOUDFLARED_TOKEN`, `GOOGLE_TTS_API_KEY`)
2. `docker compose exec app claude /login`

나머지는 모두 `deploy.bat` / `docker compose` 명령으로 자동화.
