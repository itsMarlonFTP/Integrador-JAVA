# Pruebas manuales con Swagger UI

Estas pruebas son interactivas y complementan, no sustituyen, la suite automatizada JUnit/MockMvc. Inicie la aplicación con el perfil `local`, abra `http://localhost:8080/swagger-ui.html` y despliegue las etiquetas `Product`, `Order` y `OrderItem`. H2 se reinicia al reiniciar el proceso; el script `data-local.sql` carga seis productos, tres pedidos y registros de ejemplo en los detalles, movimientos de almacén y resultados asincrónicos.

El modelo JPA está mapeado a las tablas `"Product"`, `"Order"` y `"OrderItem"`. Flyway aplica la migración V2 al iniciar y renombra las tablas sin perder datos; las auxiliares `warehouse_movements` y `async_event_logs` se conservan. Swagger UI muestra las etiquetas `Product`, `Order` y `OrderItem` (crear/consultar pedido aparece bajo Order y OrderItem). Los DTOs documentados se llaman `ProductCreateRequest`, `OrderCreateRequest`, `OrderResponse`, etc.; las entidades no se exponen. Las rutas y nombres JSON de estos ejemplos no cambiaron.

- Café molido: ID normalmente `1`, SKU `SKU-CAFE-001`, precio `125.50`, stock `20`, mínimo `5`.
- Taza térmica: ID normalmente `2`, SKU `SKU-TAZA-001`, precio `89.00`, stock `12`, mínimo `3`.
- Té verde: ID normalmente `3`, stock `3` con mínimo `4`; sirve para revisar el escenario de bajo inventario.
- Libreta reciclada: ID normalmente `4`, stock `30`, mínimo `8`.
- Botella reutilizable: ID normalmente `5`, stock `0`, mínimo `4`; no alcanza para una compra.
- Café de temporada: ID normalmente `6`, inactivo, conservado para demostrar soft delete y registros históricos.

También hay pedidos de ejemplo `1` (`EN_PROCESO`), `2` (`ENVIADO`) y `3` (`CANCELADO`). Los pedidos y movimientos incluidos son datos de demostración precargados, no operaciones emitidas durante la sesión actual.

Los IDs indicados son estables al reiniciar H2 con estos datos semilla; las nuevas altas reciben IDs posteriores. Use los IDs que devuelvan las respuestas si ya hizo pruebas. Para validar las tablas con el nuevo nombre, reinicie la app y consulte en H2 Console `"Product"`, `"Order"` y `"OrderItem"` (con comillas dobles); también verá las tablas auxiliares de auditoría/eventos. Siga este orden para que los escenarios de stock y cancelación sean repetibles.

## Escenarios

1. **Alta de producto — POST `/api/v1/products`**

   ```json
   {
     "sku": "SKU-MANUAL-001",
     "name": "Botella reutilizable",
     "description": "Botella de acero",
     "category": "Hogar",
     "price": 199.90,
     "stock": 10,
     "minimumStock": 2
   }
   ```

   Esperado: `201 Created`, representación DTO con identificador y `active: true`.

2. **Consulta de producto — GET `/api/v1/products/{id}`**

   Esperado: `200 OK` con el producto creado. Repetir con `/api/v1/products/999999` debe dar `404` y error consistente.

3. **Filtrado — GET `/api/v1/products?category=Hogar&minimumPrice=50&maximumPrice=250&page=0&size=10`**

   Esperado: `200 OK`, solo productos activos que cumplen las tres condiciones.

4. **Actualización — PUT `/api/v1/products/{id}`**

   ```json
   {
     "sku": "SKU-MANUAL-001",
     "name": "Botella premium",
     "description": "Botella de acero inoxidable",
     "category": "Hogar",
     "price": 219.90,
     "minimumStock": 3
   }
   ```

   Esperado: `200 OK`, campos actualizados y stock sin cambios. Inventario solo se modifica en endpoints específicos.

5. **Soft delete — DELETE `/api/v1/products/{id}`**

   Esperado: `204 No Content`. Después, GET del producto devuelve `404`, aparece excluido del listado y un pedido nuevo que lo incluya devolverá `404`. La información histórica permanece almacenada.

6. **Consulta y reabastecimiento — GET y POST `/api/v1/inventory/{productId}`**

   Consultar primero el stock. Ejecutar POST `/api/v1/inventory/{productId}/replenishments` con `{"quantity": 4}`. Esperado: `200 OK`, stock incrementado en cuatro.

7. **Pedido válido de varios ítems — POST `/api/v1/orders`**

   ```json
   {
     "customerId": "cliente-demo-01",
     "items": [
       { "productId": 1, "quantity": 2 },
       { "productId": 2, "quantity": 1 }
     ]
   }
   ```

   Esperado: `201 Created`, `status: "EN_PROCESO"`, total `340.00` (2 × 125.50 + 1 × 89.00), y stock reducido en 2 y 1. Los IDs corresponden a los productos iniciales si la base recién inició. No existe campo para enviar precio.

8. **Stock insuficiente — POST `/api/v1/orders`**

   Solicite para un producto una cantidad mayor a su stock actual. Esperado: `409 Conflict`; consultar nuevamente todos los productos del pedido y comprobar que ninguno tuvo descuento parcial.

9. **Precio/cantidad inválidos — POST `/api/v1/products` o `/api/v1/orders`**

   Enviar precio `0`, stock `-1`, cantidad `0` o lista de ítems vacía. Esperado: `400 Bad Request` con `timestamp`, `status`, `error`, `message` y `path`. Cantidad de reabastecimiento `0` también devuelve 400.

10. **Transiciones — PATCH `/api/v1/orders/{orderId}/status`**

    En el pedido recién creado enviar `{"status":"ENVIADO"}`; esperado `200 OK`. Intentar después `{"status":"EN_PROCESO"}` o `{"status":"CANCELADO"}` devuelve `409`. La única transición disponible por este endpoint es `EN_PROCESO` → `ENVIADO`.

11. **Cancelación, reversión única — DELETE `/api/v1/orders/{orderId}`**

    Crear otro pedido y guardar los stocks antes/después. Cancelar mientras está `EN_PROCESO`; esperado `200 OK`, estado `CANCELADO` y devolución exacta de stock. Repetir DELETE: devuelve el mismo estado, y el stock permanece igual (sin segunda devolución). Un pedido `ENVIADO` no puede cancelarse y responde 409.

12. **Inventario y auditoría — GET `/api/v1/inventory/{productId}/movements`**

    La auditoría se procesa después del commit. Consultar el historial tras operaciones de stock/pedidos; puede tardar brevemente en aparecer. Cada registro incluye tipo, delta, stock posterior y pedido relacionado cuando aplica.

13. **Notificaciones y alertas — GET `/api/v1/async-events`**

    Crear pedido y reducir un producto hasta alcanzar o cruzar su mínimo. Esperado: eventos `NOTIFICACION_COMPRA` y `ALERTA_REABASTECIMIENTO` con `successful: true`; no significa que se haya enviado un mensaje externo. Errores asincrónicos se reflejan con `successful: false` y detalle genérico, además del log de aplicación.
