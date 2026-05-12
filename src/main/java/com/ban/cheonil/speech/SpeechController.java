package com.ban.cheonil.speech;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ban.cheonil.speech.dto.SpeechRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {

  private final SpeechService speechService;

  /**
   * 음성 → 텍스트. multipart form-data 의 `audio` part 로 wav/webm/mp3 등 전송.
   *
   * @param engine STT 엔진 — "whisper" (기본) 또는 "google". 미지정 시 whisper.
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SpeechRes transcribe(
      @RequestPart("audio") MultipartFile audio,
      @RequestParam(defaultValue = "whisper") String engine) {
    SpeechService.Engine e =
        "google".equalsIgnoreCase(engine)
            ? SpeechService.Engine.GOOGLE
            : SpeechService.Engine.WHISPER;
    return speechService.transcribe(audio, e);
  }
}
