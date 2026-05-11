"""MeloTTS FastAPI 래퍼.

MeloTTS 는 Python 라이브러리만 제공하므로 얇은 HTTP 서버로 감싼다.
- 기동 시 한국어 모델 1회 로딩 (lazy → first request 지연 회피)
- POST /tts {text, speed, format} → audio bytes
- GET  /health → 헬스체크
"""

import io
import logging
import os

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field
from pydub import AudioSegment
import soundfile as sf

from melo.api import TTS

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("melotts")

LANGUAGE = os.environ.get("MELO_LANGUAGE", "KR")
DEVICE = os.environ.get("MELO_DEVICE", "cpu")

log.info("loading MeloTTS model language=%s device=%s", LANGUAGE, DEVICE)
model = TTS(language=LANGUAGE, device=DEVICE)
# 한국어 모델은 단일 화자라 첫 항목 사용. 다국어/다화자 모델 전환 시 환경변수화 검토.
SPEAKER_ID = next(iter(model.hps.data.spk2id.values()))
SAMPLING_RATE = model.hps.data.sampling_rate
log.info("model loaded. speaker_id=%s sr=%s", SPEAKER_ID, SAMPLING_RATE)

app = FastAPI(title="cheonil-melotts")


class TtsReq(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)
    speed: float = Field(1.0, ge=0.5, le=2.0)
    # normalize 후 추가 증폭 (dB). 0=기본, 3=크게(≒1.4배), 6=매우크게(≒2배).
    gain_db: int = Field(0, ge=0, le=12)
    format: str = Field("mp3", pattern="^(mp3|wav)$")


@app.get("/health")
def health():
    return {"status": "ok", "language": LANGUAGE}


@app.post("/tts")
def synthesize(req: TtsReq):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="empty text")

    wav_io = io.BytesIO()
    try:
        # MeloTTS 내부에서 soundfile.write 호출 — BytesIO 지원.
        model.tts_to_file(req.text, SPEAKER_ID, wav_io, speed=req.speed, format="wav", quiet=True)
    except Exception as e:
        log.exception("synthesis failed")
        raise HTTPException(status_code=500, detail=f"synthesis failed: {e}") from e

    wav_io.seek(0)

    if req.format == "wav":
        return Response(content=wav_io.read(), media_type="audio/wav")

    # 매장 노이즈 환경 대비 — 피크를 -0.5dBFS 근처로 정규화. 합성마다 들쭉날쭉한 레벨 평탄화 +
    # 작은 소리는 끌어올림. 클리핑 안 발생 (피크 기준 자동 스케일).
    audio = AudioSegment.from_wav(wav_io).normalize(headroom=0.5)
    if req.gain_db > 0:
        # normalize 후 추가 증폭 — 매장 시끄러울 때. 큰 음절은 clipping 가능 (≥+9dB 부터 위험).
        audio = audio + req.gain_db
    mp3_io = io.BytesIO()
    # 64kbps mono — 알림용 음성 충분, 캐시 디스크 절약.
    audio.export(mp3_io, format="mp3", bitrate="64k", parameters=["-ac", "1"])
    return Response(content=mp3_io.getvalue(), media_type="audio/mpeg")
