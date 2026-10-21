package com.mss301.payment;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.Payment;
import com.mss301.payment.entity.PaymentStatus;
import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import com.mss301.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentServiceApplicationTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
        walletRepository.save(new Wallet("C1", 5000.0));
        walletRepository.save(new Wallet("C2", 100.0));
    }

    @Test
    void testPaySuccess() {
        PaymentRequest req = new PaymentRequest(101L, "C1", 2000.0);
        Payment p = paymentService.pay(req);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.getAmount()).isEqualTo(2000.0);

        Wallet w = walletRepository.findById("C1").orElseThrow();
        assertThat(w.getBalance()).isEqualTo(3000.0);
    }

    @Test
    void testPayIdempotent() {
        PaymentRequest req = new PaymentRequest(102L, "C1", 1000.0);
        Payment p1 = paymentService.pay(req);
        Payment p2 = paymentService.pay(req);

        assertThat(p1.getOrderId()).isEqualTo(p2.getOrderId());
        Wallet w = walletRepository.findById("C1").orElseThrow();
        assertThat(w.getBalance()).isEqualTo(4000.0); // Không trừ tiền 2 lần
    }

    @Test
    void testPayWalletNotFound() {
        PaymentRequest req = new PaymentRequest(103L, "C999", 50.0);
        assertThatThrownBy(() -> paymentService.pay(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void testPayNotEnoughBalance() {
        PaymentRequest req = new PaymentRequest(104L, "C2", 1000.0);
        assertThatThrownBy(() -> paymentService.pay(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
                    assertThat(rse.getReason()).contains("Số dư không đủ: còn 100.0, cần 1000.0");
                });

        Wallet w = walletRepository.findById("C2").orElseThrow();
        assertThat(w.getBalance()).isEqualTo(100.0); // Không bị trừ
    }

    @Test
    void testRefundSuccessAndIdempotent() {
        PaymentRequest req = new PaymentRequest(105L, "C1", 1500.0);
        paymentService.pay(req);

        Wallet afterPay = walletRepository.findById("C1").orElseThrow();
        assertThat(afterPay.getBalance()).isEqualTo(3500.0);

        // Refund lần 1
        paymentService.refund(105L);
        Wallet afterRefund1 = walletRepository.findById("C1").orElseThrow();
        assertThat(afterRefund1.getBalance()).isEqualTo(5000.0);
        Payment p = paymentRepository.findById(105L).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        // Refund lần 2 -> không hoàn tiền lần 2 (idempotent)
        paymentService.refund(105L);
        Wallet afterRefund2 = walletRepository.findById("C1").orElseThrow();
        assertThat(afterRefund2.getBalance()).isEqualTo(5000.0);
    }

    @Test
    void testRefundNonExistentOrder() {
        paymentService.refund(9999L); // Không lỗi
    }
}
