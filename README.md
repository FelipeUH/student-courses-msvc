# Student Courses – Microservicios con Spring Cloud

Proyecto de práctica de una arquitectura de microservicios con **Spring Boot 3.5** y **Spring Cloud**. Gestiona cursos y estudiantes en dos servicios independientes, cada uno con su propia base de datos PostgreSQL. Los servicios se comunican entre sí mediante OpenFeign y descubrimiento de servicios con Eureka, detrás de un API Gateway.

## Arquitectura

```mermaid
flowchart LR
    C([Cliente]) --> GW["msvc-gateway<br/>:8080"]
    GW -->|"/api/courses/**<br/>lb://msvc-course"| CS["msvc-course<br/>:8091"]
    GW -->|"/api/students/**<br/>lb://msvc-student"| SS["msvc-student<br/>:8090"]
    CS -->|OpenFeign| SS
    CS --> CDB[("PostgreSQL<br/>course_db")]
    SS --> SDB[("PostgreSQL<br/>student_db")]

    EU["msvc-eureka<br/>:8761"]
    GW -.->|resuelve servicios| EU
    CS -.->|se registra| EU
    SS -.->|se registra| EU

    CFG["msvc-config<br/>:8888"]
    GW -.->|obtiene config| CFG
    CS -.->|obtiene config| CFG
    SS -.->|obtiene config| CFG
    EU -.->|obtiene config| CFG
```

Cada servicio tiene su propia base de datos y se registra en Eureka. El Gateway y el cliente Feign localizan los servicios por nombre (`lb://`), no por dirección fija.

### Ejemplo: estudiantes de un curso

`GET /api/courses/{courseId}/students` combina datos de los dos servicios:

```mermaid
sequenceDiagram
    actor C as Cliente
    participant GW as msvc-gateway
    participant EU as msvc-eureka
    participant CS as msvc-course
    participant SS as msvc-student

    C->>GW: GET /api/courses/1/students
    GW->>EU: ¿Dónde está msvc-course?
    GW->>CS: GET /api/courses/1/students
    CS->>CS: Busca el curso en course_db
    CS->>EU: ¿Dónde está msvc-student?
    CS->>SS: GET /api/students/course/1 (Feign)
    SS->>SS: Busca estudiantes en student_db
    SS-->>CS: Lista de estudiantes
    CS-->>GW: Curso + estudiantes
    GW-->>C: 200 OK
```

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `microservice-gateway` | 8080 | Punto de entrada único. Enruta `/api/courses/**` y `/api/students/**` usando Spring Cloud Gateway (WebFlux). |
| `microservice-eureka` | 8761 | Servidor de descubrimiento (Netflix Eureka). |
| `microservice-student` | 8090 | CRUD de estudiantes y consulta por curso. |
| `microservice-course` | 8091 | CRUD de cursos. Agrega los estudiantes de un curso llamando a `msvc-student` con un cliente Feign. |
| `microservice-config` | 8888 | Spring Cloud Config Server (perfil `native`). Sirve la configuración de todos los servicios desde `src/main/resources/configurations/`. |

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

### 2. Configuración

Toda la configuración está centralizada en el Config Server, en `microservice-config/src/main/resources/configurations/`:

| Archivo | Lo usa |
|---|---|
| `application.yml` | Todos los servicios (URL de Eureka) |
| `msvc-<servicio>.yml` | Solo ese servicio (puerto, base de datos, rutas del gateway…) |

El `application.yml` de cada servicio solo contiene su nombre y el `import` obligatorio del Config Server. Si el Config Server no está arriba, el servicio no arranca.

Los valores sensibles o que dependen del entorno se leen de variables de entorno. Si una variable no está definida, se usa el valor por defecto, pensado para desarrollo local:

| Variable | Por defecto | Usada por |
|---|---|---|
| `DB_HOST` | `localhost` | student, course |
| `DB_PORT` | `5432` | student, course |
| `DB_USERNAME` | `postgres` | student, course |
| `DB_PASSWORD` | `root` | student, course |
| `EUREKA_URL` | `http://localhost:8761/eureka` | todos los clientes de Eureka |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | todos los servicios |

El Config Server devuelve los `${...}` sin resolver, y cada servicio los sustituye con sus propias variables de entorno. Así, las contraseñas reales nunca se guardan en el Config Server ni en el repositorio:

```bash
DB_PASSWORD=mi-clave ./mvnw spring-boot:run
```

### 3. Arrancar los servicios (en este orden)

```bash
cd microservice-config  && ./mvnw spring-boot:run   # 1. Configuración centralizada
cd microservice-eureka  && ./mvnw spring-boot:run   # 2. Registro de servicios
cd microservice-student && ./mvnw spring-boot:run   # 3.
cd microservice-course  && ./mvnw spring-boot:run   # 4.
cd microservice-gateway && ./mvnw spring-boot:run   # 5. Punto de entrada
```

Para ver la configuración que recibe un servicio: `curl http://localhost:8888/msvc-course/default`.

Cuando todo esté arriba, el panel de Eureka en http://localhost:8761 debe mostrar `MSVC-STUDENT`, `MSVC-COURSE` y `MSVC-GATEWAY` registrados.

## Próximos pasos

- Agregar pruebas unitarias con JUnit 5 y Mockito para la capa de servicio.
- Manejo de errores con `@ControllerAdvice` (hoy un ID inexistente responde 500).
- Dockerizar los servicios con `docker-compose`.
