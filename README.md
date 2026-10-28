# Student Courses – Microservicios con Spring Cloud

Proyecto de práctica de una arquitectura de microservicios con **Spring Boot 3.5** y **Spring Cloud**. Gestiona cursos y estudiantes en dos servicios independientes, cada uno con su propia base de datos PostgreSQL. Los servicios se comunican entre sí mediante OpenFeign y descubrimiento de servicios con Eureka, detrás de un API Gateway.

## Arquitectura

```
                    ┌──────────────────────┐
  Cliente  ───────► │  msvc-gateway :8080  │
                    └──────────┬───────────┘
                               │  lb://  (balanceo vía Eureka)
              ┌────────────────┴────────────────┐
              ▼                                 ▼
   ┌─────────────────────┐   OpenFeign   ┌─────────────────────┐
   │ msvc-course  :8091  │ ────────────► │ msvc-student :8090  │
   └─────────┬───────────┘               └─────────┬───────────┘
             ▼                                     ▼
       PostgreSQL                             PostgreSQL
       course_db                              student_db

   msvc-eureka :8761  ← registro de servicios
   microservice-config :8888  ← configuración centralizada (en construcción)
```

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `microservice-gateway` | 8080 | Punto de entrada único. Enruta `/api/courses/**` y `/api/students/**` usando Spring Cloud Gateway (WebFlux). |
| `microservice-eureka` | 8761 | Servidor de descubrimiento (Netflix Eureka). |
| `microservice-student` | 8090 | CRUD de estudiantes y consulta por curso. |
| `microservice-course` | 8091 | CRUD de cursos. Agrega los estudiantes de un curso llamando a `msvc-student` con un cliente Feign. |
| `microservice-config` | 8888 | Esqueleto de Spring Cloud Config Server (aún sin configurar). |

## Tecnologías

- Java 21
- Spring Boot 3.5.7, Spring Web, Spring Data JPA / Hibernate
- Spring Cloud: Gateway, Netflix Eureka, OpenFeign, Config
- PostgreSQL
- Lombok
- Maven (proyecto multimódulo)

## Diseño de cada servicio

Los servicios de negocio siguen una arquitectura en capas:

```
controller/   → endpoints REST (@RestController)
service/      → interfaz + implementación con la lógica de negocio
persistence/  → repositorios Spring Data JPA
entities/     → entidades JPA
dto/          → objetos de transferencia (solo en msvc-course)
client/       → cliente Feign hacia msvc-student (solo en msvc-course)
```

## Endpoints (a través del gateway, puerto 8080)

### Estudiantes
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/students` | Lista todos los estudiantes |
| GET | `/api/students/{id}` | Obtiene un estudiante |
| GET | `/api/students/course/{id}` | Estudiantes inscritos en un curso |
| POST | `/api/students` | Crea un estudiante |

### Cursos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/courses` | Lista todos los cursos |
| GET | `/api/courses/{id}` | Obtiene un curso |
| GET | `/api/courses/{courseId}/students` | Curso con su profesor y la lista de estudiantes (comunicación entre servicios) |
| POST | `/api/courses` | Crea un curso |

Ejemplo de respuesta de `GET /api/courses/1/students`:

```json
{
  "courseName": "Matematicas",
  "teacherName": "Profesor A",
  "students": [
    { "id": 1, "name": "Juan", "lastName": "Perez", "email": "jua.perez@example.com", "courseId": 1 },
    { "id": 4, "name": "Ana", "lastName": "Martínez", "email": "ana.mar@example.com", "courseId": 1 }
  ]
}
```

## Cómo ejecutarlo

### 1. Base de datos

Crea las dos bases de datos y sus tablas en PostgreSQL. Los servicios usan `ddl-auto: validate`, así que las tablas deben existir antes de arrancar:

```sql
CREATE DATABASE course_db;
CREATE DATABASE student_db;

-- En course_db
CREATE TABLE courses (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255),
    teacher VARCHAR(255)
);

-- En student_db
CREATE TABLE students (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255),
    last_name VARCHAR(255),
    email     VARCHAR(255),
    course_id BIGINT
);
```

Los datos de ejemplo están en `src/main/resources/poblate.sql` de cada servicio.

Las credenciales por defecto (`postgres` / `root` en `localhost:5432`) se configuran en el `application.yml` de cada servicio.

### 2. Arrancar los servicios (en este orden)

```bash
cd microservice-eureka  && ./mvnw spring-boot:run   # 1. Registro de servicios
cd microservice-student && ./mvnw spring-boot:run   # 2.
cd microservice-course  && ./mvnw spring-boot:run   # 3.
cd microservice-gateway && ./mvnw spring-boot:run   # 4. Punto de entrada
```

Cuando todo esté arriba, el panel de Eureka en http://localhost:8761 debe mostrar `MSVC-STUDENT`, `MSVC-COURSE` y `MSVC-GATEWAY` registrados.

## Próximos pasos

- Completar el Config Server (`@EnableConfigServer` y repositorio de configuración).
- Agregar pruebas unitarias con JUnit 5 y Mockito para la capa de servicio.
- Manejo de errores con `@ControllerAdvice` (hoy un ID inexistente responde 500).
- Dockerizar los servicios con `docker-compose`.
