package com.mss301.order;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import com.mss301.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceApplicationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @BeforeEach
    void setup() {
        sagaLogRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void testCreatePendingOrder() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "P1", 2, 2000.0);
        Order order = orderService.createPendingOrder(req);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("C1");
        assertThat(order.getProductId()).isEqualTo("P1");
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getAmount()).isEqualTo(2000.0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.PENDING);
    }

    @Test
    void testUpdateSagaStatus() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "P1", 1, 1000.0);
        Order order = orderService.createPendingOrder(req);

        orderService.updateSagaStatus(order.getId(), SagaStatus.EXECUTING);
        Order updated = orderService.findById(order.getId());
        assertThat(updated.getSagaStatus()).isEqualTo(SagaStatus.EXECUTING);
    }

    @Test
    void testConfirmOrder() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "P1", 1, 1000.0);
        Order order = orderService.createPendingOrder(req);

        orderService.confirmOrder(order.getId());
        Order confirmed = orderService.findById(order.getId());
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.getSagaStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    void testMarkFailedAndCancelOrder() {
        CreateOrderRequest req = new CreateOrderRequest("C2", "P1", 1, 1000.0);
        Order order = orderService.createPendingOrder(req);

        orderService.markFailed(order.getId(), "402 - Số dư không đủ");
        Order failed = orderService.findById(order.getId());
        assertThat(failed.getSagaStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("402 - Số dư không đủ");

        orderService.cancelOrder(order.getId());
        Order cancelled = orderService.findById(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getSagaStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    void testSagaLogging() {
        CreateOrderRequest req = new CreateOrderRequest("C1", "P1", 2, 2000.0);
        Order order = orderService.createPendingOrder(req);

        orderService.log(order.getId(), "CREATE_ORDER", "SUCCESS", "Order PENDING");
        orderService.log(order.getId(), "RESERVE_INVENTORY", "SUCCESS", "Đã giữ 2 P1");
        orderService.log(order.getId(), "PROCESS_PAYMENT", "SUCCESS", "Đã thu 2000.0");
        orderService.log(order.getId(), "CONFIRM_ORDER", "SUCCESS", "Saga COMPLETED");

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(order.getId());
        assertThat(logs).hasSize(4);
        assertThat(logs.stream().map(SagaLog::getStep).toList())
                .containsExactly("CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER");
    }
}
