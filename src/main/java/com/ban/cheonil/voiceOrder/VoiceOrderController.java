package com.ban.cheonil.voiceOrder;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ban.cheonil.voiceOrder.dto.VoiceOrderCreateRes;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderReq;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/voice-order")
@RequiredArgsConstructor
public class VoiceOrderController {

  private final VoiceOrderService voiceOrderService;

  /** 사용자 발화 텍스트 → 매장/메뉴 매칭된 주문 구조 데이터 (검증/디버깅 페이지 용도). */
  @PostMapping
  public VoiceOrderRes parse(@RequestBody VoiceOrderReq req) {
    return voiceOrderService.parseToOrder(req.text());
  }

  /**
   * 음성 → 주문 생성 (end-to-end). 음성 버튼 → 녹음 → 본 endpoint 한 번 호출 → 주문 생성 + 확인 멘트 반환.
   *
   * <p>매칭 실패 (매장 미지정 / 메뉴 추측 / 사전 외 항목 등) 시 {@link VoiceOrderValidationException} → 4xx 응답으로 차단.
   */
  @PostMapping(path = "/create-order", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public VoiceOrderCreateRes createOrder(@RequestPart("audio") MultipartFile audio) {
    return voiceOrderService.createOrderFromAudio(audio);
  }
}
