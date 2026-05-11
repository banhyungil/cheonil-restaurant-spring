package com.ban.cheonil.speech;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
   * 음성 → 텍스트. multipart form-data 의 `audio` part 로 wav/webm/mp3 등 전송. consumes 를 통해 허용 Content-Type
   * 지정 가능
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SpeechRes transcribe(@RequestPart("audio") MultipartFile audio) {
    return speechService.transcribe(audio);
  }
}
