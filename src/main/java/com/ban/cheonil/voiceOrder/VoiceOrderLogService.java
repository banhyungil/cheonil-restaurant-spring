package com.ban.cheonil.voiceOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ban.cheonil.voiceOrder.entity.VoiceOrderLog;

import lombok.RequiredArgsConstructor;

/**
 * 음성 주문 로그 저장 + retention cleanup.
 *
 * <ul>
 *   <li>오디오 → {@code <baseDir>/<yyyy>/<MM>/<dd>/<uuid>.<ext>} 파일로 저장. {@link VoiceOrderLog#audioPath}
 *       에는 yyyy/MM/dd/uuid.ext 형태 상대경로 기록.
 *   <li>매일 새벽 retention 일수 초과한 row 의 파일 + DB row 함께 삭제.
 * </ul>
 *
 * <p>개인정보보호 — retention 짧게 (기본 90일). 운영자 매장에 녹음 고지 멘트 필수.
 */
@Service
@RequiredArgsConstructor
public class VoiceOrderLogService {

  private static final Logger log = LoggerFactory.getLogger(VoiceOrderLogService.class);
  private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final VoiceOrderLogRepo repo;

  @Value("${voice-order.log.dir:/data/voice-orders}")
  private String baseDirStr;

  @Value("${voice-order.log.retention-days:90}")
  private int retentionDays;

  private Path baseDir;

  @PostConstruct
  void init() throws IOException {
    this.baseDir = Path.of(baseDirStr);
    Files.createDirectories(baseDir);
    log.info("voice-order log dir={} retention={}d", baseDir, retentionDays);
  }

  /**
   * 오디오를 디스크에 저장하고 상대경로 반환. UUID 파일명이라 충돌 X. 디렉터리 부재 시 생성.
   *
   * @return 베이스 디렉터리 기준 상대경로 (예: {@code 2026/05/12/uuid.webm}). DB 에 저장될 값.
   */
  public String saveAudio(byte[] bytes, String contentType) throws IOException {
    String ext = pickExt(contentType);
    String relDir = LocalDate.now().format(DIR_FMT);
    Path dir = baseDir.resolve(relDir);
    Files.createDirectories(dir);
    String filename = UUID.randomUUID() + "." + ext;
    Path target = dir.resolve(filename);
    Files.write(target, bytes);
    return relDir + "/" + filename;
  }

  /** 호스트 전체 경로 — 운영자 UI 에서 재생할 때 InputStream 으로 open 용. */
  public Path absolutePath(String relPath) {
    return baseDir.resolve(relPath);
  }

  private static String pickExt(String mime) {
    if (mime == null) return "webm";
    String m = mime.toLowerCase();
    if (m.contains("webm")) return "webm";
    if (m.contains("ogg")) return "ogg";
    if (m.contains("mp3") || m.contains("mpeg")) return "mp3";
    if (m.contains("wav")) return "wav";
    if (m.contains("flac")) return "flac";
    return "webm";
  }

  @Transactional
  public VoiceOrderLog save(VoiceOrderLog entry) {
    return repo.save(entry);
  }

  /** 매일 04:00 — retentionDays 초과한 로그 파일 + DB row 삭제. */
  @Scheduled(cron = "${voice-order.log.cleanup-cron:0 0 4 * * *}")
  @Transactional
  public void cleanup() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
    var stale = repo.findByCreatedAtBefore(cutoff);
    if (stale.isEmpty()) {
      log.debug("voice-order cleanup: nothing to delete (cutoff={})", cutoff);
      return;
    }
    int deletedFiles = 0;
    for (VoiceOrderLog row : stale) {
      try {
        Files.deleteIfExists(absolutePath(row.getAudioPath()));
        deletedFiles++;
      } catch (IOException e) {
        log.warn("voice-order cleanup: failed to delete {}", row.getAudioPath(), e);
      }
    }
    repo.deleteAll(stale);
    log.info(
        "voice-order cleanup: removed {} rows, {} files (cutoff={})",
        stale.size(),
        deletedFiles,
        cutoff);
  }
}
