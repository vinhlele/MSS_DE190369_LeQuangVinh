package com.mss301.order.saga;

import com.mss301.order.client.InventoryClient;
import com.mss301.order.client.PaymentClient;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderSagaOrchestratorTest {

    @Autowired
    private OrderSagaOrchestrator orchestrator;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @BeforeEach
    void setup() {
        sagaLogRepository.deleteAll();
        orderRepository.deleteAll();
        reset(inventoryClient, paymentClient);
    }

    @Test
    void testHappyPath() {
        doNothing().when(inventoryClient).reserve(anyLong(), anyString(), anyInt());
        doNothing().when(paymentClient).pay(anyLong(), anyString(), anyDouble());

        CreateOrderRequest req = new CreateOrderRequest("C1", "P1", 2, 2000.0);
        Order order = orchestrator.placeOrder(req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(order.getFailureReason()).isNull();

        verify(inventoryClient).reserve(order.getId(), "P1", 2);
        verify(paymentClient).pay(order.getId(), "C1", 2000.0);
        verify(inventoryClient, never()).release(anyLong());
        verify(paymentClient, never()).refund(anyLong());

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(order.getId());
        assertThat(logs.stream().map(SagaLog::getStep).toList())
                .containsExactly("CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER");
    }

    @Test
    void testInventoryReserveFails409() {
        HttpClientErrorException conflict = HttpClientErrorException.create(
                HttpStatusCode.valueOf(409),
                "CONFLICT",
                HttpHeaders.EMPTY,
                "{\"message\":\"Không đủ hàng: còn 5, cần 100\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        doThrow(conflict).when(inventoryClient).reserve(anyLong(), anyString(), anyInt());

        CreateOrderRequest req = new CreateOrderRequest("C1", "P2", 100, 2000.0);
        Order order = orchestrator.placeOrder(req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(order.getFailureReason()).contains("409 - Không đủ hàng");

        verify(paymentClient, never()).pay(anyLong(), anyString(), anyDouble());
        verify(inventoryClient, never()).release(anyLong());

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(order.getId());
        assertThat(logs.stream().map(SagaLog::getStep).toList())
                .containsExactly("CREATE_ORDER", "SAGA", "CANCEL_ORDER");
    }

    @Test
    void testPaymentFails402CompensatesInventory() {
        doNothing().when(inventoryClient).reserve(anyLong(), anyString(), anyInt());
        doNothing().when(inventoryClient).release(anyLong());

        HttpClientErrorException paymentRequired = HttpClientErrorException.create(
                HttpStatusCode.valueOf(402),
                "PAYMENT_REQUIRED",
                HttpHeaders.EMPTY,
                "{\"message\":\"Số dư không đủ: còn 100.0, cần 1000.0\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        doThrow(paymentRequired).when(paymentClient).pay(anyLong(), anyString(), anyDouble());

        CreateOrderRequest req = new CreateOrderRequest("C2", "P1", 1, 1000.0);
        Order order = orchestrator.placeOrder(req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(order.getFailureReason()).contains("402 - Số dư không đủ");

        verify(inventoryClient).reserve(order.getId(), "P1", 1);
        verify(paymentClient).pay(order.getId(), "C2", 1000.0);
        verify(inventoryClient).release(order.getId());
        verify(paymentClient, never()).refund(anyLong());

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(order.getId());
        assertThat(logs.stream().map(SagaLog::getStep).toList())
                .containsExactly("CREATE_ORDER", "RESERVE_INVENTORY", "SAGA", "COMPENSATION", "CANCEL_ORDER");
    }

    @Test
    void testNetworkFailureServiceUnavailable() {
        doNothing().when(inventoryClient).reserve(anyLong(), anyString(), anyInt());
        doNothing().when(inventoryClient).release(anyLong());

        doThrow(new ResourceAccessException("I/O error on POST request: Connection refused"))
                .when(paymentClient).pay(anyLong(), anyString(), anyDouble());

        CreateOrderRequest req = new CreateOrderRequest("C1", "P2", 1, 20.0);
        Order order = orchestrator.placeOrder(req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(order.getFailureReason()).startsWith("Service không phản hồi: I/O error");

        verify(inventoryClient).release(order.getId());
    }

    @Test
    void testCompensationFailsRetries3TimesAndStaysCompensating() {
        doNothing().when(inventoryClient).reserve(anyLong(), anyString(), anyInt());

        // Payment thất bại
        doThrow(new ResourceAccessException("Payment failed")).when(paymentClient).pay(anyLong(), anyString(), anyDouble());

        // Compensation release cũng thất bại liên tục
        doThrow(new ResourceAccessException("Inventory release timeout")).when(inventoryClient).release(anyLong());

        CreateOrderRequest req = new CreateOrderRequest("C1", "P2", 1, 20.0);
        Order order = orchestrator.placeOrder(req);

        // Phải dừng ở PENDING / COMPENSATING, KHÔNG được đánh dấu COMPENSATED!
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.COMPENSATING);

        // Kiểm tra số lần gọi release phải là 3 lần (1 lần đầu + 2 lần retry)
        verify(inventoryClient, times(3)).release(order.getId());

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(order.getId());
        List<SagaLog> failedCompLogs = logs.stream()
                .filter(l -> "COMPENSATION".equals(l.getStep()) && "FAILED".equals(l.getResult()))
                .toList();
        assertThat(failedCompLogs).hasSize(3);

        // Không được có CANCEL_ORDER
        assertThat(logs.stream().map(SagaLog::getStep).toList())
                .doesNotContain("CANCEL_ORDER");
    }
}
