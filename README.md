# ComandaGo Backend

API REST Spring Boot para ComandaGo.

## Requisitos

- Java 17+
- PostgreSQL (o Supabase)
- Variables de entorno (ver `.env.example`)

## Configuración local

1. Copiar `src/main/resources/application-local.properties.example` a `src/main/resources/application-local.properties`
2. Completar credenciales de BD y `APP_JWT_SECRET` (mínimo 32 caracteres)
3. Ejecutar:

```bash
./mvnw spring-boot:run
```

## Primer usuario ADMIN (bootstrap)

Solo funciona cuando la base de datos **no tiene usuarios**. Crea el primer administrador y devuelve tokens (auto-login):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Administrador",
    "email": "admin@tudominio.com",
    "password": "Admin1234",
    "telefono": "3001234567"
  }'
```

La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un dígito.

Si ya existe al menos un usuario, el endpoint responde `409 Conflict`. El **personal operativo** (MESERO, COCINERO, CAJERO, RECEPCIONISTA) se gestiona con `POST /api/v1/usuarios` usando un token de administrador. **No se crean ADMIN adicionales** por ese CRUD; solo el bootstrap anterior o un flujo futuro explícito.

## Usuarios API (solo ADMIN)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/v1/usuarios` | Crear personal (roles staff; no ADMIN) |
| GET | `/api/v1/usuarios?rol=&activo=&page=&size=` | Listar paginado con filtros opcionales |
| GET | `/api/v1/usuarios/{id}` | Detalle de usuario |
| PUT | `/api/v1/usuarios/{id}` | Actualizar parcial (`nombre`, `email`, `telefono`, `rol`) |
| PATCH | `/api/v1/usuarios/{id}/password` | Reset de contraseña por admin |
| PATCH | `/api/v1/usuarios/{id}/activo` | Activar o desactivar |
| DELETE | `/api/v1/usuarios/{id}` | Borrado lógico (`activo=false`) |

Reglas de negocio:

- Solo roles **MESERO, COCINERO, CAJERO, RECEPCIONISTA** en crear/actualizar de rol.
- `DELETE` y desactivar no aplican al **último ADMIN activo**.
- Un ADMIN **no puede desactivarse a sí mismo**.
- Las contraseñas se guardan con BCrypt; nunca se exponen en respuestas.
- Al cambiar contraseña, rol o desactivar, se revocan los refresh tokens del usuario.

Ejemplo crear mesero:

```bash
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "María López",
    "email": "mesero@labrasa.com",
    "password": "Mesero1234",
    "telefono": "3001234567",
    "rol": "MESERO"
  }'
```

Verificar sesión:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <token_del_register>"
```

## Auth API

| Método | Ruta | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | Público (solo si BD vacía) |
| POST | `/api/v1/auth/login` | Público |
| POST | `/api/v1/auth/refresh` | Público |
| POST | `/api/v1/auth/logout` | Bearer |
| GET | `/api/v1/auth/me` | Bearer |
| PATCH | `/api/v1/auth/me/password` | Bearer |
| GET | `/api/v1/auth/roles` | ADMIN |

## Tests

```bash
./mvnw test
```
