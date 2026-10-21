package com.mss301.payment;

import com.mss301.payment.entity.Wallet;
import com.mss301.payment.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentServiceApplicationTests {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void contextLoadsAndSeedsWallets() {
        Optional<Wallet> c1 = walletRepository.findById("C1");
        assertThat(c1).isPresent();
        assertThat(c1.get().getBalance()).isEqualTo(5000.0);

        Optional<Wallet> c2 = walletRepository.findById("C2");
        assertThat(c2).isPresent();
        assertThat(c2.get().getBalance()).isEqualTo(100.0);
    }

}
