package com.ban.cheonil.order;

import com.ban.cheonil.order.dto.OrderCreateReq;
import com.ban.cheonil.order.dto.OrderRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderRes> create(@RequestBody OrderCreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(req));
    }
}
