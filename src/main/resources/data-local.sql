-- Datos locales de demostracion. H2 se reinicia al reiniciar la aplicacion.
-- Los IDs se generan normalmente; las relaciones se resuelven por SKU y cliente.

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-CAFE-001', 'Cafe molido', 'Bolsa de cafe de 500 g', 125.50, 20, 5, 'Abarrotes', TRUE);

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-TAZA-001', 'Taza termica', 'Taza reutilizable de 350 ml', 89.00, 12, 3, 'Hogar', TRUE);

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-TE-001', 'Te verde', 'Caja con 20 sobres', 65.00, 3, 4, 'Abarrotes', TRUE);

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-LIBRETA-001', 'Libreta reciclada', 'Libreta tamano A5 de 100 hojas', 48.50, 30, 8, 'Papeleria', TRUE);

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-BOTELLA-001', 'Botella reutilizable', 'Botella de acero inoxidable de 600 ml', 249.90, 0, 4, 'Hogar', TRUE);

INSERT INTO "Product" (sku, name, description, price, stock, minimum_stock, category, active)
VALUES ('SKU-CAFE-LEGACY', 'Cafe de temporada', 'Producto descontinuado conservado para historial',
        99.00, 2, 2, 'Abarrotes', FALSE);

INSERT INTO "Order" (customer_id, status, total, created_at, version)
VALUES ('cliente-demo-001', 'EN_PROCESO', 340.00,
        TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:00+00:00', 0);

INSERT INTO "Order" (customer_id, status, total, created_at, version)
VALUES ('cliente-demo-002', 'ENVIADO', 130.00,
        TIMESTAMP WITH TIME ZONE '2026-10-24 15:30:00+00:00', 0);

INSERT INTO "Order" (customer_id, status, total, created_at, version)
VALUES ('cliente-demo-003', 'CANCELADO', 97.00,
        TIMESTAMP WITH TIME ZONE '2026-10-23 09:00:00+00:00', 0);

INSERT INTO "OrderItem" (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, p.id, 2, 125.50, 251.00
FROM "Order" o JOIN "Product" p ON p.sku = 'SKU-CAFE-001'
WHERE o.customer_id = 'cliente-demo-001';

INSERT INTO "OrderItem" (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, p.id, 1, 89.00, 89.00
FROM "Order" o JOIN "Product" p ON p.sku = 'SKU-TAZA-001'
WHERE o.customer_id = 'cliente-demo-001';

INSERT INTO "OrderItem" (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, p.id, 2, 65.00, 130.00
FROM "Order" o JOIN "Product" p ON p.sku = 'SKU-TE-001'
WHERE o.customer_id = 'cliente-demo-002';

INSERT INTO "OrderItem" (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, p.id, 2, 48.50, 97.00
FROM "Order" o JOIN "Product" p ON p.sku = 'SKU-LIBRETA-001'
WHERE o.customer_id = 'cliente-demo-003';

INSERT INTO warehouse_movements (product_id, type, quantity_delta, stock_after, order_id, occurred_at)
SELECT p.id, 'VENTA', -2, 20, o.id, TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:01+00:00'
FROM "Product" p JOIN "Order" o ON o.customer_id = 'cliente-demo-001'
WHERE p.sku = 'SKU-CAFE-001';

INSERT INTO warehouse_movements (product_id, type, quantity_delta, stock_after, order_id, occurred_at)
SELECT p.id, 'VENTA', -1, 12, o.id, TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:01+00:00'
FROM "Product" p JOIN "Order" o ON o.customer_id = 'cliente-demo-001'
WHERE p.sku = 'SKU-TAZA-001';

INSERT INTO warehouse_movements (product_id, type, quantity_delta, stock_after, order_id, occurred_at)
SELECT p.id, 'VENTA', -2, 3, o.id, TIMESTAMP WITH TIME ZONE '2026-10-24 15:30:01+00:00'
FROM "Product" p JOIN "Order" o ON o.customer_id = 'cliente-demo-002'
WHERE p.sku = 'SKU-TE-001';

INSERT INTO warehouse_movements (product_id, type, quantity_delta, stock_after, order_id, occurred_at)
SELECT p.id, 'VENTA', -2, 28, o.id, TIMESTAMP WITH TIME ZONE '2026-10-23 09:00:01+00:00'
FROM "Product" p JOIN "Order" o ON o.customer_id = 'cliente-demo-003'
WHERE p.sku = 'SKU-LIBRETA-001';

INSERT INTO warehouse_movements (product_id, type, quantity_delta, stock_after, order_id, occurred_at)
SELECT p.id, 'DEVOLUCION_CANCELACION', 2, 30, o.id,
       TIMESTAMP WITH TIME ZONE '2026-10-23 09:05:00+00:00'
FROM "Product" p JOIN "Order" o ON o.customer_id = 'cliente-demo-003'
WHERE p.sku = 'SKU-LIBRETA-001';

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('NOTIFICACION_COMPRA', 'Dato de demostracion del pedido cliente-demo-001; no se envio un mensaje externo',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('AUDITORIA_INVENTARIO', 'Dato de demostracion: movimiento VENTA de SKU-CAFE-001; pedido cliente-demo-001',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('AUDITORIA_INVENTARIO', 'Dato de demostracion: movimiento VENTA de SKU-TAZA-001; pedido cliente-demo-001',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-25 10:15:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('NOTIFICACION_COMPRA', 'Dato de demostracion del pedido cliente-demo-002; no se envio un mensaje externo',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-24 15:30:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('AUDITORIA_INVENTARIO', 'Dato de demostracion: movimiento VENTA de SKU-TE-001; pedido cliente-demo-002',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-24 15:30:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('ALERTA_REABASTECIMIENTO', 'Dato de demostracion: alerta simulada para SKU-TE-001 con stock 3 (minimo 4)',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-24 15:30:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('NOTIFICACION_COMPRA', 'Dato de demostracion del pedido cliente-demo-003; no se envio un mensaje externo',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-23 09:00:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('AUDITORIA_INVENTARIO', 'Dato de demostracion: movimiento VENTA de SKU-LIBRETA-001; pedido cliente-demo-003',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-23 09:00:02+00:00');

INSERT INTO async_event_logs (event_type, details, successful, occurred_at)
VALUES ('AUDITORIA_INVENTARIO',
        'Dato de demostracion: movimiento DEVOLUCION_CANCELACION de SKU-LIBRETA-001; pedido cliente-demo-003',
        TRUE, TIMESTAMP WITH TIME ZONE '2026-10-23 09:05:01+00:00');
