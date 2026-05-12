package com.ban.cheonil.speech;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 임의 오디오 포맷 → WAV 16kHz mono LINEAR16 표준화.
 *
 * <p>모바일/브라우저가 보내는 컨테이너가 다양함 (webm/m4a/mp3/aac/wav 등) + MIME 메타데이터가 실제 데이터와 mismatch 인 케이스도 있음
 * (예: Android 가 m4a 를 audio/mpeg 로 표기). 정규화 한 번 거치면 Whisper/Google STT 둘 다 일관되게 인식.
 *
 * <p>구현: ffmpeg subprocess. stdin → stdout 파이프로 임시 파일 X.
 *
 * <p>정규화 실패 시 {@link IOException} — 호출자는 fallback 으로 원본 사용을 고려할 수 있음.
 */
@Component
public class AudioNormalizer {

  private static final Logger log = LoggerFactory.getLogger(AudioNormalizer.class);

  /** ffmpeg 한 번 호출 timeout — 1분 발화도 보통 1초 안에 변환. */
  private static final int TIMEOUT_SECONDS = 30;

  /** 입력 오디오를 WAV 16kHz mono PCM (LINEAR16) 으로 변환. */
  public byte[] toWav16kMono(byte[] input) throws IOException {
    if (input == null || input.length == 0) {
      throw new IOException("input audio is empty");
    }

    ProcessBuilder pb =
        new ProcessBuilder(
            "ffmpeg",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            "pipe:0",
            "-ar",
            "16000",
            "-ac",
            "1",
            "-f",
            "wav",
            "pipe:1");
    pb.redirectErrorStream(false);

    Process p;
    try {
      p = pb.start();
    } catch (IOException e) {
      throw new IOException("ffmpeg launch failed (binary missing?): " + e.getMessage(), e);
    }

    // stdin 쓰기는 별도 스레드 (stdout 읽기와 동시 진행되어야 deadlock 방지).
    Thread stdinWriter =
        new Thread(
            () -> {
              try (OutputStream os = p.getOutputStream()) {
                os.write(input);
                os.flush();
              } catch (IOException ignored) {
                // ffmpeg 가 입력 닫았을 수도 있음 — 무시.
              }
            },
            "ffmpeg-stdin");
    stdinWriter.setDaemon(true);
    stdinWriter.start();

    // stdout / stderr 동시 읽기.
    byte[] output;
    String stderr;
    try (InputStream stdout = p.getInputStream();
        InputStream stderrStream = p.getErrorStream()) {
      // stderr 도 별도 스레드 — pipe 차오르면 ffmpeg block 됨.
      StderrReader err = new StderrReader(stderrStream);
      err.start();
      output = stdout.readAllBytes();
      err.join(TimeUnit.SECONDS.toMillis(2));
      stderr = err.text();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      p.destroyForcibly();
      throw new IOException("interrupted while reading ffmpeg output", e);
    }

    try {
      if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        p.destroyForcibly();
        throw new IOException("ffmpeg timeout after " + TIMEOUT_SECONDS + "s");
      }
      stdinWriter.join(2_000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("interrupted while waiting for ffmpeg", e);
    }

    int exit = p.exitValue();
    if (exit != 0) {
      throw new IOException("ffmpeg failed (exit=" + exit + "): " + stderr);
    }
    log.debug("audio normalized: in={} bytes → out={} bytes", input.length, output.length);
    return output;
  }

  /** stderr 비동기 수집 — pipe block 방지 + 에러 메시지 보존. */
  private static final class StderrReader extends Thread {
    private final InputStream stream;
    private volatile String text = "";

    StderrReader(InputStream stream) {
      super("ffmpeg-stderr");
      setDaemon(true);
      this.stream = stream;
    }

    @Override
    public void run() {
      try {
        text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException ignored) {
        // stream 닫혔으면 OK.
      }
    }

    String text() {
      return text;
    }
  }
}
