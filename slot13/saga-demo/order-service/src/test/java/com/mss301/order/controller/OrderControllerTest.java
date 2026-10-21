package com.mss301.order.controller;

import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.Order;
import com.mss301.order.entity.OrderStatus;
import com.mss301.order.entity.SagaLog;
import com.mss301.order.entity.SagaStatus;
import com.mss301.order.repository.OrderRepository;
import com.mss301.order.repository.SagaLogRepository;
import com.mss301.order.saga.OrderSagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderSagaOrchestrator orchestrator;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private SagaLogRepository sagaLogRepository;

    @Test
    void testPlaceOrderHappyPath() throws Exception {
        Order completedOrder = new Order();
        completedOrder.setId(1L);
        completedOrder.setCustomerId("C1");
        completedOrder.setProductId("P1");
        completedOrder.setQuantity(2);
        completedOrder.setAmount(2000.0);
        completedOrder.setStatus(OrderStatus.CONFIRMED);
        completedOrder.setSagaStatus(SagaStatus.COMPLETED);

        when(orchestrator.placeOrder(any(CreateOrderRequest.class))).thenReturn(completedOrder);

        String jsonRequest = """
                {
                    "customerId": "C1",
                    "productId": "P1",
                    "quantity": 2,
                    "amount": 2000.0
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerId", is("C1")))
                .andExpect(jsonPath("$.productId", is("P1")))
                .andExpect(jsonPath("$.quantity", is(2)))
                .andExpect(jsonPath("$.amount", is(2000.0)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.sagaStatus", is("COMPLETED")));
    }

    @Test
    void testPlaceOrderFailurePath() throws Exception {
        Order failedOrder = new Order();
        failedOrder.setId(2L);
        failedOrder.setCustomerId("C2");
        failedOrder.setProductId("P1");
        failedOrder.setQuantity(1);
        failedOrder.setAmount(1000.0);
        failedOrder.setStatus(OrderStatus.CANCELLED);
        failedOrder.setSagaStatus(SagaStatus.COMPENSATED);
        failedOrder.setFailureReason("402 - Số dư không đủ: còn 100, cần 1000");

        when(orchestrator.placeOrder(any(CreateOrderRequest.class))).thenReturn(failedOrder);

        String jsonRequest = """
                {
                    "customerId": "C2",
                    "productId": "P1",
                    "quantity": 1,
                    "amount": 1000.0
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.sagaStatus", is("COMPENSATED")))
                .andExpect(jsonPath("$.failureReason", is("402 - Số dư không đủ: còn 100, cần 1000")));
    }

    @Test
    void testFindAllOrders() throws Exception {
        Order o1 = new Order();
        o1.setId(1L);
        o1.setCustomerId("C1");
        o1.setStatus(OrderStatus.CONFIRMED);

        Order o2 = new Order();
        o2.setId(2L);
        o2.setCustomerId("C2");
        o2.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void testGetSagaLogs() throws Exception {
        SagaLog l1 = new SagaLog();
        l1.setId(10L);
        l1.setOrderId(1L);
        l1.setStep("CREATE_ORDER");
        l1.setResult("SUCCESS");
        l1.setMessage("Order created");

        SagaLog l2 = new SagaLog();
        l2.setId(11L);
        l2.setOrderId(1L);
        l2.setStep("RESERVE_INVENTORY");
        l2.setResult("SUCCESS");
        l2.setMessage("Reserved 2 of P1");

        when(sagaLogRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(l1, l2));

        mockMvc.perform(get("/api/orders/1/saga-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].step", is("CREATE_ORDER")))
                .andExpect(jsonPath("$[0].result", is("SUCCESS")))
                .andExpect(jsonPath("$[1].step", is("RESERVE_INVENTORY")))
                .andExpect(jsonPath("$[1].result", is("SUCCESS")));
    }
}
