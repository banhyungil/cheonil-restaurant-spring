package com.ban.cheonil.voiceOrder;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ban.cheonil.voiceOrder.dto.VoiceOrderReq;
import com.ban.cheonil.voiceOrder.dto.VoiceOrderRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/voice-order")
@RequiredArgsConstructor
public class VoiceOrderController {

  private final VoiceOrderService voiceOrderService;

  /** 사용자 발화 텍스트 → 매장/메뉴 매칭된 주문 구조 데이터. */
  @PostMapping
  public VoiceOrderRes parse(@RequestBody VoiceOrderReq req) {
    return voiceOrderService.parse(req.text());
  }
}
