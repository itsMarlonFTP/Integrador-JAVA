package com.axity.curso.retailservice;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.axity.curso.retailservice.repository.AsyncEventLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RetailserviceApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncEventLogRepository asyncEventLogs;

    @Test
    void completeProductInventoryAndOrderWorkflow() throws Exception {
        long firstProduct = createProduct("SKU-TEST-A", "Producto A", "Alimentos", "12.50", 5, 1);
        long secondProduct = createProduct("SKU-TEST-B", "Producto B", "Hogar", "3.00", 4, 3);

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Alimentos")
                        .param("minimumPrice", "10.00")
                        .param("maximumPrice", "20.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-TEST-A"));

        mockMvc.perform(put("/api/v1/products/{id}", firstProduct)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-TEST-A","name":"Producto A actualizado","description":"Descripción",
                                 "category":"Alimentos","price":13.25,"minimumStock":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(13.25))
                .andExpect(jsonPath("$.stock").value(5));

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"cliente-test","items":[
                                  {"productId":%d,"quantity":2},
                                  {"productId":%d,"quantity":1}
                                ]}
                                """.formatted(firstProduct, secondProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_PROCESO"))
                .andExpect(jsonPath("$.total").value(29.50))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();
        long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/inventory/{id}", firstProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
        mockMvc.perform(get("/api/v1/inventory/{id}", secondProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> asyncEventLogs.count() >= 4);
        mockMvc.perform(get("/api/v1/async-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItem("NOTIFICACION_COMPRA")))
                .andExpect(jsonPath("$.content[*].eventType", hasItem("AUDITORIA_INVENTARIO")))
                .andExpect(jsonPath("$.content[*].eventType", hasItem("ALERTA_REABASTECIMIENTO")));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"cliente-test","items":[
                                  {"productId":%d,"quantity":99},
                                  {"productId":%d,"quantity":1}
                                ]}
                                """.formatted(firstProduct, secondProduct)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(get("/api/v1/inventory/{id}", secondProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENVIADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"));
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EN_PROCESO\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isConflict());

        long cancellableOrder = createOrder(firstProduct, 1);
        mockMvc.perform(delete("/api/v1/orders/{id}", cancellableOrder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        mockMvc.perform(get("/api/v1/inventory/{id}", firstProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
        mockMvc.perform(delete("/api/v1/orders/{id}", cancellableOrder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        mockMvc.perform(get("/api/v1/inventory/{id}", firstProduct))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));

        mockMvc.perform(post("/api/v1/inventory/{id}/replenishments", firstProduct)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
        mockMvc.perform(post("/api/v1/inventory/{id}/adjustments", firstProduct)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityDelta\":-1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(4));

        mockMvc.perform(get("/api/v1/inventory/{id}/movements", firstProduct))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/{id}", firstProduct))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products/{id}", firstProduct))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Alimentos")
                        .param("minimumPrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"cliente-test","items":[{"productId":%d,"quantity":1}]}
                                """.formatted(firstProduct)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/products/999999"));
    }

    @Test
    void invalidRequestsReturnConsistentBadRequest() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("RetailOrder Service API"))
                .andExpect(jsonPath("$.info.description", containsString("\"Order\"")))
                .andExpect(jsonPath("$.components.schemas.OrderCreateRequest.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.OrderResponse.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.ProductCreateRequest.type").value("object"))
                .andExpect(jsonPath("$.paths['/api/v1/products'].get.tags",
                        org.hamcrest.Matchers.contains("Product")))
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.tags",
                        org.hamcrest.Matchers.containsInAnyOrder("Order", "OrderItem")))
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post").exists());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-INVALID","name":"Inválido","description":null,
                                 "category":"Test","price":0,"stock":-1,"minimumStock":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("price")));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"cliente-test\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentOrdersCannotOversellInventory() throws Exception {
        long productId = createProduct("SKU-CONCURRENT", "Producto concurrente", "Prueba", "10.00", 1, 0);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> placeConcurrentOrder(productId, ready, start));
            Future<Integer> second = executor.submit(() -> placeConcurrentOrder(productId, ready, start));
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Las solicitudes concurrentes no quedaron listas");
            }
            start.countDown();
            int[] statuses = {first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)};
            Arrays.sort(statuses);
            assertArrayEquals(new int[] {201, 409}, statuses);
            mockMvc.perform(get("/api/v1/inventory/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(0));
        } finally {
            executor.shutdownNow();
        }
    }

    private long createProduct(String sku, String name, String category, String price, int stock, int minimum)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"%s","description":"Descripción de prueba",
                                 "category":"%s","price":%s,"stock":%d,"minimumStock":%d}
                                """.formatted(sku, name, category, price, stock, minimum)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private long createOrder(long productId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"cliente-cancelacion","items":[
                                  {"productId":%d,"quantity":%d}
                                ]}
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private int placeConcurrentOrder(long productId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("No se liberó el inicio de la solicitud concurrente");
        }
        return mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"cliente-concurrente","items":[
                                  {"productId":%d,"quantity":1}
                                ]}
                                """.formatted(productId)))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
