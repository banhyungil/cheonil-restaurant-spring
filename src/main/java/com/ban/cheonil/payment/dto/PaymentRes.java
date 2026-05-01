package com.ban.cheonil.payment.dto;

import java.time.OffsetDateTime;

import com.ban.cheonil.payment.entity.PayType;
import com.ban.cheonil.payment.entity.Payment;

public record PaymentRes(
    Long seq, Long orderSeq, Integer amount, PayType payType, OffsetDateTime payAt) {
  public static PaymentRes from(Payment p) {
    return new PaymentRes(p.getSeq(), p.getOrderSeq(), p.getAmount(), p.getPayType(), p.getPayAt());
  }
}
