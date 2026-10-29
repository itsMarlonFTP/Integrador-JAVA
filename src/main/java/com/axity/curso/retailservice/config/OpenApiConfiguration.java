package com.axity.curso.retailservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class OpenApiConfiguration {
    @Bean
    OpenAPI retailOrderOpenApi() {
        return new OpenAPI()
                .info(new Info()
                .title("RetailOrder Service API")
                .version("v1")
                .description("API REST para catálogo, inventario y pedidos. Los precios se calculan en el servidor. "
                        + "Los modelos JPA internos Product, Order y OrderItem se exponen mediante DTOs documentados, "
                        + "no como entidades. Las tablas de dominio se llaman \"Product\", \"Order\" y \"OrderItem\". "
                        + "La auditoría y los resultados asincrónicos permanecen en sus tablas auxiliares."));
    }
}
