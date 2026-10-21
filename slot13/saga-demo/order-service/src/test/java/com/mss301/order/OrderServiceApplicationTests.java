package com.mss301.order;

import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceApplicationTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Test
    void testSaveOrderAndSagaLog() {
        Order order = new Order();
        order.setCustomerId("C1");
        order.setProductId("P1");
        order.setQuantity(2);
        order.setAmount(2000.0);
        order.setStatus(OrderStatus.PENDING);
        order.setSagaStatus(SagaStatus.PENDING);
        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();

        SagaLog log1 = new SagaLog(saved.getId(), "CREATE_ORDER", "SUCCESS", "Order PENDING");
        SagaLog log2 = new SagaLog(saved.getId(), "RESERVE_INVENTORY", "SUCCESS", "Đã giữ 2 P1");
        sagaLogRepository.save(log1);
        sagaLogRepository.save(log2);

        List<SagaLog> logs = sagaLogRepository.findByOrderIdOrderByIdAsc(saved.getId());
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getStep()).isEqualTo("CREATE_ORDER");
        assertThat(logs.get(1).getStep()).isEqualTo("RESERVE_INVENTORY");
    }

}
