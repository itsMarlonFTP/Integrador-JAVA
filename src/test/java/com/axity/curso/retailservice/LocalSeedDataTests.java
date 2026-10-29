package com.axity.curso.retailservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.axity.curso.retailservice.model.Product;
import com.axity.curso.retailservice.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("local")
class LocalSeedDataTests {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductRepository products;

    @Test
    void localProfileSeedsRelatedRecordsAndKeepsGeneratedIdsAvailable() {
        assertEquals(6L, count("\"Product\""));
        assertEquals(3L, count("\"Order\""));
        assertEquals(4L, count("\"OrderItem\""));
        assertEquals(5L, count("warehouse_movements"));
        assertEquals(9L, count("async_event_logs"));
        assertEquals(2L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"version\" IN ('1', '2') AND \"success\" = TRUE",
                Long.class));

        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Order\" o "
                        + "LEFT JOIN \"OrderItem\" i ON i.order_id = o.id "
                        + "WHERE i.id IS NULL", Long.class));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"OrderItem\" i "
                        + "LEFT JOIN \"Product\" p ON p.id = i.product_id "
                        + "WHERE p.id IS NULL", Long.class));

        Product created = products.saveAndFlush(new Product("SKU-AUTO-ID", "Producto de prueba",
                "Validación de identidad", new BigDecimal("10.00"), 1, "Pruebas"));
        assertTrue(created.getId() > 6L);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
