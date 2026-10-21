package com.mss301.inventory;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.entity.Product;
import com.mss301.inventory.entity.Reservation;
import com.mss301.inventory.entity.ReservationStatus;
import com.mss301.inventory.repository.ProductRepository;
import com.mss301.inventory.repository.ReservationRepository;
import com.mss301.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InventoryServiceApplicationTests {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        productRepository.deleteAll();
        productRepository.saveAll(List.of(
                new Product("P1", "Laptop", 1000, 10),
                new Product("P2", "Mouse", 20, 5)
        ));
    }

    @Test
    void testReserveSuccess() {
        ReserveRequest req = new ReserveRequest(1001L, "P1", 2);
        Reservation r = inventoryService.reserve(req);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(r.getQuantity()).isEqualTo(2);

        Product p1 = productRepository.findById("P1").orElseThrow();
        assertThat(p1.getStock()).isEqualTo(8);
    }

    @Test
    void testReserveIdempotent() {
        ReserveRequest req = new ReserveRequest(1002L, "P1", 2);
        Reservation r1 = inventoryService.reserve(req);
        Reservation r2 = inventoryService.reserve(req);

        assertThat(r1.getOrderId()).isEqualTo(r2.getOrderId());
        Product p1 = productRepository.findById("P1").orElseThrow();
        assertThat(p1.getStock()).isEqualTo(8); // Không bị trừ 2 lần
    }

    @Test
    void testReserveProductNotFound() {
        ReserveRequest req = new ReserveRequest(1003L, "P999", 1);
        assertThatThrownBy(() -> inventoryService.reserve(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void testReserveNotEnoughStock() {
        ReserveRequest req = new ReserveRequest(1004L, "P2", 10);
        assertThatThrownBy(() -> inventoryService.reserve(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        Product p2 = productRepository.findById("P2").orElseThrow();
        assertThat(p2.getStock()).isEqualTo(5); // Kho không đổi
    }

    @Test
    void testReleaseSuccessAndIdempotent() {
        ReserveRequest req = new ReserveRequest(1005L, "P1", 3);
        inventoryService.reserve(req);

        Product afterReserve = productRepository.findById("P1").orElseThrow();
        assertThat(afterReserve.getStock()).isEqualTo(7);

        // Release lần 1
        inventoryService.release(1005L);
        Product afterRelease1 = productRepository.findById("P1").orElseThrow();
        assertThat(afterRelease1.getStock()).isEqualTo(10);
        Reservation res = reservationRepository.findById(1005L).orElseThrow();
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.RELEASED);

        // Release lần 2 -> không tăng kho lần 2 (idempotent)
        inventoryService.release(1005L);
        Product afterRelease2 = productRepository.findById("P1").orElseThrow();
        assertThat(afterRelease2.getStock()).isEqualTo(10);
    }

    @Test
    void testReleaseNonExistentOrder() {
        // Release order chưa từng reserve không gây lỗi
        inventoryService.release(9999L);
    }
}
