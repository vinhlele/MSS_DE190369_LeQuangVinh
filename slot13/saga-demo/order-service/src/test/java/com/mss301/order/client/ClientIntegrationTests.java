package com.mss301.order.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
class ClientIntegrationTests {

    private InventoryClient inventoryClient;
    private PaymentClient paymentClient;
    private MockRestServiceServer mockInventoryServer;
    private MockRestServiceServer mockPaymentServer;

    @BeforeEach
    void setup() {
        RestClient.Builder invBuilder = RestClient.builder().baseUrl("http://localhost:8082");
        mockInventoryServer = MockRestServiceServer.bindTo(invBuilder).build();
        inventoryClient = new InventoryClient(invBuilder.build());

        RestClient.Builder payBuilder = RestClient.builder().baseUrl("http://localhost:8083");
        mockPaymentServer = MockRestServiceServer.bindTo(payBuilder).build();
        paymentClient = new PaymentClient(payBuilder.build());
    }

    @Test
    void testInventoryReserveSuccess() {
        mockInventoryServer.expect(requestTo("http://localhost:8082/api/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess());

        inventoryClient.reserve(1L, "P1", 2);
        mockInventoryServer.verify();
    }

    @Test
    void testInventoryReserveConflict409() {
        mockInventoryServer.expect(requestTo("http://localhost:8082/api/inventory/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"message\":\"Không đủ hàng\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> inventoryClient.reserve(2L, "P2", 100))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        mockInventoryServer.verify();
    }

    @Test
    void testInventoryRelease() {
        mockInventoryServer.expect(requestTo("http://localhost:8082/api/inventory/release/1"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("RELEASED", MediaType.TEXT_PLAIN));

        inventoryClient.release(1L);
        mockInventoryServer.verify();
    }

    @Test
    void testPaymentPaySuccess() {
        mockPaymentServer.expect(requestTo("http://localhost:8083/api/payments/pay"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess());

        paymentClient.pay(1L, "C1", 2000.0);
        mockPaymentServer.verify();
    }

    @Test
    void testPaymentPayPaymentRequired402() {
        mockPaymentServer.expect(requestTo("http://localhost:8083/api/payments/pay"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).body("{\"message\":\"Số dư không đủ\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> paymentClient.pay(2L, "C2", 1000.0))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        mockPaymentServer.verify();
    }

    @Test
    void testPaymentRefund() {
        mockPaymentServer.expect(requestTo("http://localhost:8083/api/payments/refund/1"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("REFUNDED", MediaType.TEXT_PLAIN));

        paymentClient.refund(1L);
        mockPaymentServer.verify();
    }
}
