package com.axity.curso.retailservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import com.axity.curso.retailservice.dto.ProductDtos.CreateRequest;
import com.axity.curso.retailservice.dto.ProductDtos.Response;
import com.axity.curso.retailservice.dto.ProductDtos.UpdateRequest;
import com.axity.curso.retailservice.exception.ConflictoOperacionException;
import com.axity.curso.retailservice.exception.PrecioInvalidoException;
import com.axity.curso.retailservice.exception.RecursoNoEncontradoException;
import com.axity.curso.retailservice.model.Product;
import com.axity.curso.retailservice.repository.ProductRepository;
import com.axity.curso.retailservice.service.event.RetailAsyncEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class ProductService {
    private final ProductRepository products;
    private final ApplicationEventPublisher events;

    public ProductService(ProductRepository products, ApplicationEventPublisher events) {
        this.products = products;
        this.events = events;
    }

    @Transactional
    public Response create(CreateRequest request) {
        validatePrice(request.price());
        if (products.existsBySkuIgnoreCase(request.sku())) {
            throw new ConflictoOperacionException("Ya existe un producto con ese SKU");
        }
        Product product = new Product(request.sku().trim(), request.name().trim(), request.description(),
                request.price(), request.stock(), request.category());
        product.setMinimumStock(request.minimumStock());
        product = products.saveAndFlush(product);
        if (product.getStock() <= product.getMinimumStock()) {
            publishLowStock(product);
        }
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public Page<Response> search(String category, BigDecimal minimumPrice, BigDecimal maximumPrice,
            Pageable pageable) {
        if (minimumPrice != null) {
            validatePriceFilter(minimumPrice);
        }
        if (maximumPrice != null) {
            validatePriceFilter(maximumPrice);
        }
        if (minimumPrice != null && maximumPrice != null && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new IllegalArgumentException("El precio mínimo no puede superar el precio máximo");
        }
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        Specification<Product> filters = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            if (normalizedCategory != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        normalizedCategory.toLowerCase(Locale.ROOT)));
            }
            if (minimumPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minimumPrice));
            }
            if (maximumPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maximumPrice));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return products.findAll(filters, pageable)
                .map(ProductService::toResponse);
    }

    @Transactional
    public Response update(Long id, UpdateRequest request) {
        validatePrice(request.price());
        Product product = products.findActiveForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
        if (!product.getSku().equalsIgnoreCase(request.sku())
                && products.existsBySkuIgnoreCase(request.sku())) {
            throw new ConflictoOperacionException("Ya existe un producto con ese SKU");
        }
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setPrice(request.price());
        int previousMinimumStock = product.getMinimumStock();
        product.setMinimumStock(request.minimumStock());
        if (product.getStock() > previousMinimumStock && product.getStock() <= product.getMinimumStock()) {
            publishLowStock(product);
        }
        return toResponse(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = products.findActiveForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
        product.setActive(false);
    }

    Product findActive(Long id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    static Response toResponse(Product product) {
        return new Response(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getStock(), product.getMinimumStock(),
                product.isActive());
    }

    static void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2 || price.precision() > 10) {
            throw new PrecioInvalidoException("El precio debe ser mayor que cero y tener hasta dos decimales");
        }
    }

    private static void validatePriceFilter(BigDecimal price) {
        if (price.signum() < 0 || price.scale() > 2 || price.precision() > 10) {
            throw new IllegalArgumentException("El filtro de precio debe ser no negativo y tener hasta dos decimales");
        }
    }

    void publishMovement(Product product, String type, int delta, Long orderId) {
        events.publishEvent(new RetailAsyncEvent("WAREHOUSE_AUDIT", orderId, product.getId(), delta,
                product.getStock(), product.getMinimumStock(), type));
    }

    void publishIfThresholdCrossed(Product product, int previousStock) {
        if (previousStock > product.getMinimumStock() && product.getStock() <= product.getMinimumStock()) {
            publishLowStock(product);
        }
    }

    void publishLowStock(Product product) {
        events.publishEvent(new RetailAsyncEvent("LOW_STOCK", null, product.getId(), null,
                product.getStock(), product.getMinimumStock(), null));
    }
}
