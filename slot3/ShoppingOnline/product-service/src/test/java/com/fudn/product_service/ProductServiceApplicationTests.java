package com.fudn.product_service;

import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.model.Product;
import com.fudn.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

    @Container
    static MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.mongodb.uri",
                mongoDBContainer::getReplicaSetUrl
        );
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JsonMapper jsonMapper;

    @BeforeEach
    void cleanup() {
        productRepository.deleteAll();
    }

    // ==========================================
    // TEST 1: Context
    // ==========================================

    @Test
    void contextLoads() {
    }

    // ==========================================
    // TEST 2: Create Product
    // ==========================================

    @Test
    void shouldCreateProduct() throws Exception {

        ProductRequest productRequest = new ProductRequest(
                "Test Product",
                "This is a test product",
                BigDecimal.valueOf(19.99)
        );

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(productRequest))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description")
                        .value("This is a test product"))
                .andExpect(jsonPath("$.price").value(19.99));

        // Check database
        assertThat(productRepository.findAll())
                .hasSize(1);
    }

    // ==========================================
    // TEST 3: Get All Products
    // ==========================================

    @Test
    void shouldGetAllProducts() throws Exception {

        // Arrange
        Product product1 = Product.builder()
                .name("Product 1")
                .description("Description 1")
                .price(BigDecimal.valueOf(10.99))
                .build();

        Product product2 = Product.builder()
                .name("Product 2")
                .description("Description 2")
                .price(BigDecimal.valueOf(20.99))
                .build();

        productRepository.save(product1);
        productRepository.save(product2);

        // Act & Assert
        mockMvc.perform(
                        get("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Product 1"))
                .andExpect(jsonPath("$[0].description")
                        .value("Description 1"))
                .andExpect(jsonPath("$[0].price").value(10.99))
                .andExpect(jsonPath("$[1].name").value("Product 2"))
                .andExpect(jsonPath("$[1].description")
                        .value("Description 2"))
                .andExpect(jsonPath("$[1].price").value(20.99));
    }

    // ==========================================
    // TEST 4: Get All Products When Empty
    // ==========================================

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() throws Exception {

        mockMvc.perform(
                        get("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==========================================
    // TEST 5: Create Multiple Products
    // ==========================================

    @Test
    void shouldCreateMultipleProducts() throws Exception {

        ProductRequest product1 = new ProductRequest(
                "Laptop",
                "Gaming Laptop",
                BigDecimal.valueOf(1500)
        );

        ProductRequest product2 = new ProductRequest(
                "Mouse",
                "Wireless Mouse",
                BigDecimal.valueOf(50)
        );

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(product1))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(product2))
                )
                .andExpect(status().isCreated());

        // Check database
        assertThat(productRepository.findAll())
                .hasSize(2);
    }
}