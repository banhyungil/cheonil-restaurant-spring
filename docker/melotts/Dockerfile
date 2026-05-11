# MeloTTS 자체 호스팅 — Whisper 컨테이너와 동일한 self-hosted 패턴.
# 베이스: Debian slim + Python 3.10. CPU 추론 전용 (GPU 불필요).
#
# 빌드:  docker build -t cheonil-melotts ./docker/melotts
# 실행:  docker run --rm -p 9100:8000 cheonil-melotts
#
# 첫 합성 호출 시 HuggingFace 에서 한국어 모델 자동 다운로드(~1GB).
# /root/.cache/huggingface 를 named volume 으로 마운트해 영속화.
FROM python:3.10-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    DEBIAN_FRONTEND=noninteractive

# - ffmpeg: pydub mp3 인코딩
# - libsndfile1: soundfile WAV 입출력
# - git: pip 가 MeloTTS 를 GitHub 에서 직접 clone
# - build-essential: 일부 의존성(예: g2pkk 등) 빌드 시 필요
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libsndfile1 \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

# MeloTTS 의 text/cleaner.py 가 모든 언어 모듈을 import 하므로 한국어만 쓰더라도
# japanese.py 가 로드되어 MeCab+unidic 사전을 요구한다. 미리 다운로드 (~750MB).
RUN python -m unidic download

# MeloTTS 영문 phoneme 파이프라인이 첫 호출 시 nltk 데이터를 받음 — 빌드 타임에 미리 다운로드.
RUN python -c "import nltk; nltk.download('averaged_perceptron_tagger_eng', quiet=True)" || true

COPY server.py .

EXPOSE 8000
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
