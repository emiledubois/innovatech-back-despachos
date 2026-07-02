# Back Despachos — Innovatech Chile

API REST para la gestión de despachos y logística de entregas, desarrollada con Spring Boot 3 como parte del sistema de ventas de Innovatech Chile.

---

## Arquitectura

```
GitHub (push a main)
       │
       ▼
GitHub Actions (CI/CD)
  ├── Build & Test (Maven)
  ├── Docker build & push → Amazon ECR
  └── Deploy → AWS ECS Fargate
                   │
             ┌─────┴─────┐
             │ ECS Task  │
             │ :8081     │
             └─────┬─────┘
                   │
              RDS MySQL
```

**Stack:**
- Java 17 + Spring Boot 3.4.4
- Spring Data JPA + Hibernate
- MySQL (AWS RDS)
- SpringDoc OpenAPI (Swagger UI)
- Docker multietapa (Maven → JRE Alpine)
- AWS ECS Fargate + Amazon ECR
- CI/CD con GitHub Actions

---

## Endpoints disponibles

Base URL: `http://<ALB-DNS>/api/v1/despachos`  
Swagger UI: `http://<ALB-DNS>/swagger-ui.html`

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/despachos` | Listar todos los despachos |
| `GET` | `/api/v1/despachos/{idDespacho}` | Obtener despacho por ID |
| `POST` | `/api/v1/despachos` | Crear nuevo despacho |
| `PUT` | `/api/v1/despachos/{idDespacho}` | Actualizar despacho existente |
| `DELETE` | `/api/v1/despachos/{idDespacho}` | Eliminar despacho por ID |

### Ejemplo de body (POST/PUT)

```json
{
  "fechaDespacho": "2025-07-15",
  "patenteCamion": "BBBB22",
  "intento": 0,
  "idCompra": 1,
  "direccionCompra": "Av. Providencia 1234, Santiago",
  "valorCompra": 35000,
  "despachado": false
}
```

---

## Vriables de entorno

El servicio requiere las siguientes variables de entorno para conectarse a la base de datos. **No hay credenciales hardcodeadas en el código.**

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_ENDPOINT` | Host del RDS MySQL | `innovatech-db.xxxx.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | Puerto de la base de datos | `3306` |
| `DB_NAME` | Nombre de la base de datos | `despachos` |
| `DB_USERNAME` | Usuario MySQL | `admin` |
| `DB_PASSWORD` | Contraseña MySQL | *(guardada en GitHub Secrets)* |

---

## Correr localmente con Docker

```bash
# 1. Clonar el repositorio
git clone https://github.com/<tu-usuario>/innovatech-back-despachos.git
cd innovatech-back-despachos/Springboot-API-REST-DESPACHO

# 2. Build de la imagen
docker build -t back-despachos:local .

# 3. Correr el contenedor
docker run -d -p 8081:8081 \
  -e DB_ENDPOINT=localhost \
  -e DB_PORT=3306 \
  -e DB_NAME=despachos \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root \
  back-despachos:local

# 4. Verificar
curl http://localhost:8081/api/v1/despachos
# Swagger UI: http://localhost:8081/swagger-ui.html
```

### Con Maven directamente (sin Docker)

```bash
cd Springboot-API-REST-DESPACHO

# Ejecutar tests
./mvnw clean verify

# Correr la aplicación
export DB_ENDPOINT=localhost
export DB_PORT=3306
export DB_NAME=despachos
export DB_USERNAME=root
export DB_PASSWORD=root
./mvnw spring-boot:run
```

---

## Pipeline CI/CD (GitHub Actions)

El pipeline se encuentra en `.github/workflows/ci-cd.yml` y se activa automáticamente con cada `push` a la rama `main`.

### Flujo completo

```
push a main
    │
    ▼
[Job 1] build-and-test
    ├── Checkout código
    ├── Setup Java 17
    └── mvn clean verify (tests incluidos)
    │
    ▼
[Job 2] build-and-push-ecr
    ├── Configurar credenciales AWS
    ├── Login en Amazon ECR
    └── docker build + tag + push (tag: SHA del commit + latest)
    │
    ▼
[Job 3] deploy-ecs
    ├── Obtener Task Definition actual
    ├── Actualizar imagen en Task Definition
    └── Registrar nueva TD y forzar redeploy en ECS
```

### Secrets requeridos en GitHub

Ir a **Settings → Secrets and variables → Actions** y agregar:

| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Access Key de AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Secret Key de AWS Academy |
| `AWS_SESSION_TOKEN` | Session Token (renovar cada ~4h en Academy) |
| `AWS_REGION` | `us-east-1` |
| `AWS_ACCOUNT_ID` | ID de tu cuenta AWS (12 dígitos) |

---

## Infraestructura AWS

| Recurso | Valor |
|---|---|
| Clúster | `innovatech-cluster` (ECS Fargate) |
| Servicio ECS | `back-despachos` |
| Task Definition | `innovatech-back-despachos` |
| Imagen ECR | `<account>.dkr.ecr.us-east-1.amazonaws.com/innovatech/back-despachos` |
| Puerto | `8081` |
| CPU / Memoria | 512 vCPU / 1024 MB |
| Logs | CloudWatch `/ecs/innovatech/back-despachos` |
| Autoscaling | Target Tracking 50% CPU — mín. 1, máx. 4 tareas |

### Ver logs en producción

```bash
# Seguir logs en tiempo real
aws logs tail /ecs/innovatech/back-despachos --follow --region us-east-1

# Filtrar errores
aws logs filter-log-events \
  --log-group-name /ecs/innovatech/back-despachos \
  --filter-pattern "ERROR" \
  --region us-east-1
```

---

## Estructura del proyecto

```
Springboot-API-REST-DESPACHO/
├── src/
│   ├── main/
│   │   ├── java/com/citt/
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java          # Configuración CORS
│   │   │   │   └── OpenApiConfig.java       # Configuración Swagger
│   │   │   ├── controller/
│   │   │   │   └── DespachoController.java  # Endpoints REST
│   │   │   ├── exceptions/
│   │   │   │   ├── DespachoNotFoundException.java
│   │   │   │   └── RestResponseEntityExceptionHandler.java
│   │   │   ├── persistence/
│   │   │   │   ├── entity/Despacho.java     # Entidad JPA
│   │   │   │   ├── repository/DespachoRepository.java
│   │   │   │   └── services/
│   │   │   │       ├── DespachoService.java
│   │   │   │       └── DespachoServiceImpl.java
│   │   │   └── SpringbootApiRestDespachoApplication.java
│   │   └── resources/
│   │       └── application.properties       # Config con variables de entorno
│   └── test/
│       └── java/com/citt/
│           └── SpringbootApiRestDespachoApplicationTests.java
├── Dockerfile
├── pom.xml
└── .github/
    └── workflows/
        └── ci-cd.yaml
```
