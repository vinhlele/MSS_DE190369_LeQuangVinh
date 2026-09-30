package com.fudn.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        value = "inventory",
        url = "${inventory.url}"
)
public interface InventoryClient {

    @GetMapping("/api/inventory")
    boolean isInStock(
            @RequestParam String skuCode,
            @RequestParam Integer quantity
    );
}