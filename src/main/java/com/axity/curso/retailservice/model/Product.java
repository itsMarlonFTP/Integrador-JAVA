package com.axity.curso.retailservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Representa un producto del catálogo y sus existencias disponibles.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del producto dentro del catálogo.
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    /**
     * Precio positivo, con hasta ocho dígitos enteros y dos decimales.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Cantidad disponible; se permiten cero unidades, pero no valores negativos.
     */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock;

    /**
     * Categoría expresada como texto libre.
     */
    @Size(max = 50)
    @Column(length = 50)
    private String category;

    /**
     * Indica si el producto está disponible en el catálogo.
     * Se establece en true para los productos nuevos.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Constructor requerido por JPA.
     */
    protected Product() {
    }

    /**
     * Crea un producto nuevo. El identificador se genera al persistirlo.
     *
     * @param sku código único del producto
     * @param name nombre del producto
     * @param description descripción opcional
     * @param price precio unitario
     * @param stock existencias iniciales
     * @param category categoría opcional en texto libre
     */
    public Product(
            String sku,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            String category) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.active = true;
    }

    /**
     * Devuelve el identificador generado del producto.
     *
     * @return identificador, o null antes de persistir
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el SKU del producto.
     *
     * @return SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Actualiza el SKU del producto.
     *
     * @param sku nuevo SKU
     */
    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * Devuelve el nombre del producto.
     *
     * @return nombre
     */
    public String getName() {
        return name;
    }

    /**
     * Actualiza el nombre del producto.
     *
     * @param name nuevo nombre
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Devuelve la descripción del producto.
     *
     * @return descripción, o null si no se definió
     */
    public String getDescription() {
        return description;
    }

    /**
     * Actualiza la descripción del producto.
     *
     * @param description nueva descripción
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Devuelve el precio unitario.
     *
     * @return precio
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Actualiza el precio unitario.
     *
     * @param price nuevo precio
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Devuelve las existencias disponibles.
     *
     * @return cantidad en stock
     */
    public Integer getStock() {
        return stock;
    }

    /**
     * Actualiza las existencias disponibles.
     *
     * @param stock nueva cantidad en stock
     */
    public void setStock(Integer stock) {
        this.stock = stock;
    }

    /**
     * Devuelve la categoría de texto libre.
     *
     * @return categoría, o null si no se definió
     */
    public String getCategory() {
        return category;
    }

    /**
     * Actualiza la categoría de texto libre.
     *
     * @param category nueva categoría
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Indica si el producto está activo en el catálogo.
     *
     * @return true si está activo
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Cambia el estado de disponibilidad del producto.
     * Para la eliminación lógica, se debe establecer en false.
     *
     * @param active nuevo estado
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}