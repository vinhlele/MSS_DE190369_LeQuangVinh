package com.fudn.orderservice;

import com.fudn.orderservice.stub.InventoryStubs;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EnableWireMock(
        @ConfigureWireMock(
                baseUrlProperties = "inventory.url",
                port = 0
        )
)
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer mySQLContainer =
            new MySQLContainer("mysql:8.3.0");

    static {
        mySQLContainer.start();
    }

    @LocalServerPort
    private Integer port;

    @Autowired
    private Environment environment;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        System.out.println("======================================");
        System.out.println("ORDER SERVICE PORT = " + port);
        System.out.println(
                "INVENTORY URL      = "
                        + environment.getProperty("inventory.url")
        );
        System.out.println("======================================");
    }

    @Test
    void shouldSubmitOrder() {

        String submitOrderJson = """
                {
                    "skuCode": "iphone_15",
                    "price": 1000,
                    "quantity": 1
                }
                """;

        // Inventory giả lập trả true
        InventoryStubs.stubInventoryCall(
                "iphone_15",
                1
        );

        String responseBody = RestAssured
                .given()
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        assertThat(
                responseBody,
                Matchers.is("Order Placed Successfully")
        );

        // Verify Order Service thực sự gọi Inventory
        verify(
                getRequestedFor(
                        urlEqualTo(
                                "/api/inventory?skuCode=iphone_15&quantity=1"
                        )
                )
        );
    }

    @Test
    void shouldFailOrderWhenProductIsNotInStock() {

        String submitOrderJson = """
                {
                    "skuCode": "iphone_15",
                    "price": 1000,
                    "quantity": 1000
                }
                """;

        // Inventory giả lập trả false
        InventoryStubs.stubInventoryOutOfStock(
                "iphone_15",
                1000
        );

        RestAssured
                .given()
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .log().all()
                .statusCode(500);
    }
}