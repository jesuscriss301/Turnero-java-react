# Turnero SaaS — MVP desplegable (Dokploy)

SaaS multi-tenant de gestión de filas virtuales. Monolito **Spring Boot 3 / Java 21** que sirve
la API y el frontend **React + Vite + TypeScript** compilado, con **MySQL 8** y **Flyway**.
Un solo contenedor de aplicación + base de datos → ideal para un VPS con **Dokploy**.

## Qué incluye este MVP
- Registro autoservicio de empresa (tenant) → plan **FREE**: 1 sucursal, vigencia 3 meses.
- Multi-tenancy pool: todas las consultas acotadas por `tenant_id` (repos + JWT).
- Catálogo: sucursales, servicios (prefijo, prioridad opcional), puntos de atención con
  **matriz de compatibilidad** punto ↔ servicios.
- Emisión de turnos (recepción/tablet y base para QR/web) con código `PREFIJO-NNN`
  (secuencia diaria atómica por servicio vía `LAST_INSERT_ID`).
- Despacho **"siguiente compatible"** con `SELECT ... FOR UPDATE SKIP LOCKED`
  (dos puntos nunca reciben el mismo turno).
- Ciclo de vida: WAITING → CALLED → IN_SERVICE → FINISHED / ABSENT / CANCELLED, con re-llamado
  y bitácora `ticket_event`.
- **Tiempo real por SSE**: pantalla pública (`/display/{branchId}?key=...`) y seguimiento
  móvil del cliente (`/q/{token}`), con reconexión automática.
- Plan FREE: zona publicitaria del **40%** en pantalla pública; al expirar los 3 meses se
  bloquea la emisión de turnos (upgrade manual: `UPDATE tenant SET plan='PAID'`).
- Seguridad: JWT (HS256), BCrypt, tokens públicos opacos por ticket, clave por pantalla.

## Estructura
```
backend/    Spring Boot (API + estáticos del frontend en el jar final)
frontend/   React + Vite + TS (build copiado al jar en el Dockerfile)
Dockerfile  Multi-stage: node → maven → JRE 21 (usuario no root)
docker-compose.yml  app + mysql (volumen persistente, healthcheck)
```

## Despliegue en Dokploy (VPS)
1. **Instala Dokploy** en tu VPS (Ubuntu limpio):
   `curl -sSL https://dokploy.com/install.sh | sh` y entra a `http://IP:3000`.
2. Sube este proyecto a un repositorio **Git** (GitHub/GitLab/Gitea).
3. En Dokploy: **Create Project → Add Service → Compose**, conecta el repo,
   rama `main`, Compose Path `./docker-compose.yml`.
4. En **Environment** del servicio, define (ver `.env.example`):
   - `DB_PASSWORD`, `DB_ROOT_PASSWORD`
   - `APP_JWT_SECRET` (genera: `openssl rand -base64 64 | tr -d '\n'`)
5. En **Domains**: añade tu dominio apuntando (DNS A) a la IP del VPS,
   Service Name = `app`, Container Port = `8080`, y activa HTTPS (Let's Encrypt).
   Dokploy enruta con su Traefik; no necesitas exponer puertos ni Nginx propio.
6. **Deploy**. El primer build compila frontend y backend (5–10 min).
   Flyway crea el esquema automáticamente al arrancar.

## Prueba end-to-end (2 minutos)
1. `https://tu-dominio/register` → crea tu empresa.
2. En **Admin**: crea sucursal → servicios (ej. "Pagos", prefijo `P`) → punto "Caja 1"
   marcando los servicios compatibles.
3. Abre **Pantalla ↗** (enlace con `key`) en otra pestaña/TV.
4. En **Recepción** emite un turno → copia el enlace `/q/...` en el móvil.
5. En **Operador**: pulsa **Siguiente ▶** → la pantalla y el móvil se actualizan al instante
   (SSE). Inicia y finaliza la atención.

## Desarrollo local
```
docker compose up mysql -d           # o tu MySQL local
cd backend && mvn spring-boot:run    # API en :8080
cd frontend && npm i && npm run dev  # UI en :5173 (proxy /api → :8080)
```

## Operación
- Health: `GET /actuator/health`.
- Backup MySQL (desde el VPS):
  `docker exec <mysql> mysqldump -u root -p"$DB_ROOT_PASSWORD" turnero > backup.sql`
  (programa un cron + copia a object storage; ver Blueprint E-29).
- Escalado: la app es stateless salvo el hub SSE en memoria → antes de una 2ª instancia,
  introducir Redis pub/sub (previsto en Blueprint ADR-005/E-27).

## Deuda consciente vs. Blueprint (siguientes iteraciones)
- Refresh tokens/OIDC completo, roles finos y gestión de usuarios del tenant.
- Branding pago (logo/colores), i18n react-i18next, SSR/SEO de páginas públicas.
- Emisión pública por QR sin login (endpoint público + rate limiting), sonido en pantalla,
  QR gráfico en el ticket, métricas operativas, pagos.
