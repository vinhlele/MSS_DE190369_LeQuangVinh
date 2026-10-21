package com.mss301.payment.controller;

import com.mss301.payment.entity.Payment;
import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.PaymentRepository;
import com.mss301.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/wallets")
    public List<Wallet> wallets() {
        return walletRepository.findAll();
    }

    @GetMapping
    public List<Payment> payments() {
        return paymentRepository.findAll();
    }
}
