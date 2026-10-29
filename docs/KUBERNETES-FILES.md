# Docker y Kubernetes: archivos y demostracion

Esta guia relaciona los archivos de la actividad con las fases del PDF
"Proyecto Integrador Kubernetes - Solucionario (Docker Desktop)". Los comandos
estan escritos para **CMD de Windows**, como la terminal usada durante la
practica. Ejecutalos desde la raiz del repositorio.

La arquitectura desplegada es:

```text
Cliente / Swagger
       |
Service retail-api (puerto 80)
       |
Deployment retail-api (3 replicas, puerto 8080)
       |
Service postgres (puerto 5432)
       |
Deployment PostgreSQL (1 replica) + PVC postgres-data
```

PostgreSQL es una instancia compartida: las tres replicas de la API consultan
los mismos productos y pedidos. Flyway crea el esquema al iniciar la API; los
datos demostrativos se cargan manualmente.

## Archivos de la actividad

### `Dockerfile`

Construye la imagen de la API en dos etapas. Maven y Java 21 compilan el
proyecto y generan el JAR; una imagen de ejecucion Java 21 mas pequena contiene
solo ese JAR. El contenedor publica el puerto `8080`.

La version corregida que esta desplegada es `retail-order-service:1.0.1`.
Kubernetes toma la etiqueta que aparece en `k8s/api-deployment.yaml`.

### `.dockerignore`

Excluye del contexto de `docker build` archivos que no hacen falta en la imagen
final, como `.git`, `target`, la documentacion y configuracion de VS Code. Esto
reduce el contexto enviado al motor de Docker.

### `k8s/postgres.yaml`

Declara tres recursos:

- **PVC `postgres-data`:** solicita 1 GiB de almacenamiento persistente.
- **Service `postgres`:** da a los pods de la API un nombre DNS interno estable
  para conectarse al puerto `5432`.
- **Deployment `postgres`:** inicia PostgreSQL 17 con una replica, lee la
  contrasena del Secret y monta el PVC como directorio de datos.

La base tiene una replica porque el manifiesto no configura replicacion de
PostgreSQL. El PVC conserva los datos al reemplazar el pod; no es un respaldo.

### `k8s/api-configmap.yaml`

Define las variables no confidenciales de la API:

- `SPRING_PROFILES_ACTIVE=postgres` activa el perfil de PostgreSQL.
- `DB_URL` apunta al Service interno `postgres`.
- `DB_USERNAME` define el usuario de conexion.

La contrasena no se guarda en el ConfigMap.

### `k8s/api-deployment.yaml`

Crea tres replicas del microservicio con la imagen `retail-order-service:1.0.1`.
Configura el puerto `8080`, importa el ConfigMap y obtiene `DB_PASSWORD` del
Secret `retail-db-secret`. Tambien establece probes de inicio, disponibilidad y
vida, y solicitudes y limites de recursos.

### `k8s/api-service.yaml`

Crea el Service `retail-api`, que selecciona las tres replicas y publica el
puerto `80` hacia el `8080` de la API. Es de tipo `LoadBalancer`. Si la
direccion externa que asigna Docker Desktop no resulta accesible desde Windows,
`kubectl port-forward` permite acceder localmente.

### `k8s/postgres-seed.sql`

Carga los datos de demostracion equivalentes a
`src/main/resources/data-local.sql`: seis productos, tres pedidos, cuatro
detalles, cinco movimientos y nueve eventos asincronos. Resuelve relaciones
por SKU y cliente en lugar de depender de IDs fijos.

El script se ejecuta manualmente despues de que Flyway haya creado las tablas.
No es idempotente: **no lo ejecutes de nuevo sobre la base ya sembrada**, pues
puede duplicar registros o fallar por SKU duplicados.

### Archivos de codigo relacionados con el error de PostgreSQL

Durante la prueba se corrigio la busqueda de productos para que PostgreSQL no
interprete como `bytea` los parametros de filtro nulos:

- `src/main/java/com/axity/curso/retailservice/service/ProductService.java`
  construye los filtros solo cuando el usuario los proporciona.
- `src/main/java/com/axity/curso/retailservice/repository/ProductRepository.java`
  permite ejecutar la busqueda dinamica mediante Specifications.
- `src/test/java/com/axity/curso/retailservice/RetailserviceApplicationTests.java`
  verifica el listado sin filtros, con categoria y con rango de precios.

Este cambio se construyo en la imagen `1.0.1`; la imagen `1.0.0` tenia el error
del listado y no debe usarse para demostrar el funcionamiento correcto.

## Secret de Kubernetes

Los manifiestos esperan un Secret llamado `retail-db-secret`, con la clave
`DB_PASSWORD`. Se crea por separado y no se versiona un archivo con la
contrasena:

```cmd
kubectl create secret generic retail-db-secret --from-literal=DB_PASSWORD=retail_dev --dry-run=client -o yaml | kubectl apply -f -
```

`retail_dev` es una credencial local de demostracion; no es para produccion.
Un Secret de Kubernetes no cifra automaticamente sus datos en Base64. Para
comprobar que existe sin imprimir su valor:

```cmd
kubectl get secret retail-db-secret
```

## Demostracion por fases del PDF

### Fase 0: entorno local Docker Desktop

Desde CMD, en la raiz del repositorio:

```cmd
docker version
kubectl config current-context
kubectl cluster-info
kubectl get nodes
```

La salida debe confirmar que Docker tiene cliente y servidor, el contexto es
`docker-desktop`, el control plane responde y el nodo tiene estado `Ready`.

### Fase 1: imagen del microservicio

Construir la misma etiqueta que usa el Deployment:

```cmd
docker build -t retail-order-service:1.0.1 .
docker image ls retail-order-service
```

La primera orden compila el JAR y crea la imagen. La segunda debe mostrar la
etiqueta `1.0.1`. En Docker Desktop, abre **Images** y busca
`retail-order-service:1.0.1`.

> La imagen `1.0.1` ya fue construida y desplegada durante esta practica. No es
> necesario reconstruirla salvo que cambie el codigo o se borre la imagen.

### Fase 2: Deployment, tres replicas y rollback

Comprobar el despliegue actual:

```cmd
kubectl get deployment retail-api
kubectl get pods -l app=retail-api -o wide
kubectl rollout status deployment/retail-api --timeout=600s
kubectl rollout history deployment/retail-api
```

La evidencia debe mostrar `READY 3/3` en el Deployment y tres pods `Running` y
`Ready`. En Docker Desktop, abre la vista **Kubernetes** y muestra el
Deployment `retail-api` y sus tres pods.

El PDF pide practicar `rollout undo`. La version anterior `1.0.0` de esta API
tenia el error de PostgreSQL, asi que **no ejecutes directamente el rollback
ahora**. Para demostrar el mecanismo de forma segura, primero se puede crear
una segunda etiqueta para el mismo binario ya corregido y revertirla:

```cmd
docker tag retail-order-service:1.0.1 retail-order-service:1.0.2
kubectl set image deployment/retail-api retail-api=retail-order-service:1.0.2
kubectl rollout status deployment/retail-api --timeout=600s
kubectl rollout history deployment/retail-api
kubectl rollout undo deployment/retail-api
kubectl rollout status deployment/retail-api --timeout=600s
kubectl get deployment retail-api
```

Esto demuestra una actualizacion y rollback sin volver a una version con el
error. El manifiesto `k8s/api-deployment.yaml` continua declarando `1.0.1`;
despues del rollback, el Deployment debe volver a esa imagen. Esta prueba es
opcional si solo solicitan mostrar el historial.

### Fase 3: ConfigMap y Secret

Mostrar los valores no confidenciales y verificar la existencia del Secret:

```cmd
kubectl get configmap retail-api-config -o yaml
kubectl get secret retail-db-secret
kubectl exec deployment/retail-api -- printenv SPRING_PROFILES_ACTIVE
kubectl exec deployment/retail-api -- printenv DB_URL
```

Las dos ultimas ordenes deben mostrar `postgres` y
`jdbc:postgresql://postgres:5432/retaildb`. No imprimas `DB_PASSWORD` en una
captura o reporte.

En Docker Desktop, muestra el Deployment y sus pods; la configuracion se puede
demostrar con los comandos anteriores. La interfaz grafica puede variar segun
la version de Docker Desktop.

### Fase 4: PVC y persistencia de PostgreSQL

Comprobar el reclamo:

```cmd
kubectl get pvc postgres-data
kubectl describe pvc postgres-data
```

El estado esperado es `Bound` y la capacidad solicitada es `1Gi`. Docker
Desktop administra la clase de almacenamiento local; en este clúster el PVC se
vincula a la clase `standard`.

Como el seed ya se ejecuto, la persistencia se puede demostrar reemplazando el
pod de PostgreSQL y volviendo a consultar los datos:

```cmd
kubectl delete pod -l app=postgres
kubectl rollout status deployment/postgres --timeout=180s
kubectl get pods -l app=postgres
echo SELECT count(*) AS productos FROM "Product"; | kubectl exec -i deployment/postgres -- psql -U retail_dev -d retaildb
```

El conteo debe continuar en `6`. **Elimina el pod, nunca el PVC**, para esta
prueba. En Docker Desktop, muestra el pod de PostgreSQL recreado y el volumen
persistente/PVC. La prueba de conteo en `psql` confirma que se preservaron los
registros.

Tambien se pueden verificar los otros conteos:

```cmd
echo SELECT count(*) AS pedidos FROM "Order"; | kubectl exec -i deployment/postgres -- psql -U retail_dev -d retaildb
echo SELECT count(*) AS detalles FROM "OrderItem"; | kubectl exec -i deployment/postgres -- psql -U retail_dev -d retaildb
echo SELECT count(*) AS movimientos FROM warehouse_movements; | kubectl exec -i deployment/postgres -- psql -U retail_dev -d retaildb
echo SELECT count(*) AS eventos FROM async_event_logs; | kubectl exec -i deployment/postgres -- psql -U retail_dev -d retaildb
```

Se esperan `3`, `4`, `5` y `9`, respectivamente.

### Fase 5: Service y acceso HTTP

Mostrar el Service:

```cmd
kubectl get service retail-api
kubectl describe service retail-api
```

La salida muestra tipo `LoadBalancer`, puerto `80` y endpoints de los pods de
la API. Si el campo `EXTERNAL-IP` es accesible desde Windows, se puede probar
esa IP con el navegador. El acceso comprobado para esta practica es el
reenvio local:

```cmd
kubectl port-forward service/retail-api 8080:80
```

Deja esa ventana de CMD abierta. En otra ventana, verifica la API:

```cmd
curl.exe -i http://localhost:8080/api/v1/products
curl.exe -i "http://localhost:8080/api/v1/products?category=Hogar"
```

Las dos solicitudes deben responder `HTTP/1.1 200`. El listado general
devuelve cinco productos activos; el sexto producto de la base esta inactivo y
por eso no aparece. Abre <http://localhost:8080/swagger-ui.html> para mostrar
Swagger UI y probar los endpoints manualmente.

En Docker Desktop, muestra el Service `retail-api`, su Deployment y los tres
pods. El navegador o `curl.exe` prueba que el Service lleva trafico hasta la
API.

### Fase 6: observabilidad y operacion con `kubectl`

Comandos del PDF adaptados a las etiquetas reales del proyecto:

```cmd
kubectl get pods -l app=retail-api -o wide
kubectl describe pods -l app=retail-api
kubectl logs -l app=retail-api --tail=50 --prefix=true
kubectl rollout history deployment/retail-api
```

Para abrir una terminal dentro de un pod, primero lista los nombres y sustituye
`NOMBRE_DEL_POD` por uno de los pods de la API:

```cmd
kubectl get pods -l app=retail-api
kubectl exec -it NOMBRE_DEL_POD -- sh
```

Escribe `exit` para salir. En Docker Desktop se pueden mostrar el estado de
pods y Deployments; `describe` y `logs` aportan la evidencia detallada desde la
consola.

El PDF describe un proxy sidecar como concepto avanzado de Service Mesh. Esta
actividad **no instala un Service Mesh ni agrega un sidecar**: no se necesita
para contenerizar, desplegar, configurar, persistir o exponer este
microservicio. Si la evaluacion exige un sidecar ejecutable, eso seria un
requisito adicional al despliegue implementado.
