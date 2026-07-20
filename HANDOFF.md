# HANDOFF — session-cluster

> Ушбу ҳужжат ишни бошқа сессияда (масалан Claude Desktop'да) давом эттириш учун.
> Сана: 2026-07-20. Ёзган: олдинги Claude Code сессияси.

## Қисқача: нима қилинди

Лойиҳа — Spring Boot 4 веб-иловани 2 нусхада (`app1`, `app2`) nginx ортида
ишлатиб, HTTP-сессияни ташқи **Hazelcast кластерида** сақлаш демоси
(session clustering). Топшириқ: лойиҳани **ишга тушириш** эди.

Ишга туширишда 5 та тўсиқ чиқди, ҳаммаси тузатилди. Ҳозир **бутун стек
ишлаяпти** ва браузерда логин ҳам тўғри ишлайди (сервер логларида тасдиқланган).

Илова: **http://localhost:8080** · логин: `admin` / `admin123`

## Муҳим қарор (архитектура ўзгарди)

**Spring Boot 4 → Spring Session 4.0 `spring-session-hazelcast` модулини олиб
ташлаган** (BOM'да фақат core/redis/jdbc қолган). Шу боис эски механизм
(`@EnableHazelcastHttpSession` + `spring-session-hazelcast`) Boot 4'да умуман
қурилмайди.

Фойдаланувчи «Spring Boot 4 қолсин» деди. Танланган ечим (Native Boot 4):
`spring-session-core`'даги умумий `MapSessionRepository`'ни Hazelcast client
`IMap` устига қўйдик (`@EnableSpringHttpSession`). IMap distributed бўлгани учун
сессия барибир ташқи, умумий Hazelcast store'да қолади — премиса ўзгармади.
Камчилиги (демо учун аҳамиятсиз): EntryProcessor delta-update ва principal-name
индекс йўқолади.

## Ўзгартирилган файллар ва сабаблари

1. **`app/pom.xml`** — `spring-session-hazelcast` → `spring-session-core`
   (версиясиз, Boot 4 BOM бошқаради). `maven-dependency-plugin`'да
   `includeArtifactIds` → фақат `spring-session-core`.

2. **`app/src/main/java/com/example/demo/config/SessionConfig.java`** —
   `@EnableHazelcastHttpSession` → `@EnableSpringHttpSession`; янги
   `SessionRepository` bean = `MapSessionRepository` (Hazelcast `IMap` устида).

3. **`app/src/main/resources/application.yml`** —
   - эски `spring.session.store-type/hazelcast` блоки олинди (энди йўқ модул);
   - `hazelcast.session.map-name` + `timeout-minutes` қўшилди;
   - `server.forward-headers-strategy: framework` (nginx ортида тўғри redirect);
   - `server.servlet.session.tracking-modes: cookie` (URL'даги `;SESSION=` ўчди).

4. **`hazelcast/hazelcast.xml`** — сессия map `in-memory-format`: `OBJECT` →
   **`BINARY`**. Сабаб: OBJECT'да сервер объектни deserialize қилиб, унда
   `MapSession` + spring-security класслари бўлишини талаб қиларди. BINARY'да
   сервер фақат байт сақлайди — ҳеч қандай Spring jar керак эмас.

5. **`app/src/main/java/com/example/demo/user/DataSeeder.java`** — race-safe
   қилинди. app1 ва app2 бир вақтда `admin`'ни қўшиб `duplicate key` берарди;
   энди `DataIntegrityViolationException` хотиржам ютилади.

6. **`docker-compose.yml`** — postgres volume mount `/var/lib/postgresql/data`
   → **`/var/lib/postgresql`** (Postgres 18'нинг янги каталог конвенцияси).

7. **`nginx/nginx.conf`** — `Host $host` (порт тушиб қоларди) → **`$http_host`**
   + `X-Forwarded-Host/Port`. Аввал логиндан кейин портсиз `localhost/login`'га
   отиб, браузер «This site can't be reached» берарди.

8. **`app/src/main/java/com/example/demo/config/SecurityConfig.java`** —
   `/error`'ни `permitAll`'га қўшдим. Энг охирги (энг қийин) баг шу эди:
   браузер `favicon.ico`'ни cookie'сиз сўраб, у 404 → `/error` (ҳимояланган) →
   Spring рақобатчи сессия яратарди → форма CSRF токени бир сессияда, браузер
   cookie'си бошқасида → `POST /login`'да «Invalid CSRF» (403) → логинга қайтиш.

> Эслатма: тахлил вақтида вақтинча `docker-compose.override.yml` (security DEBUG
> лог) ишлатилди — ҳозир **ўчирилган**. hz-ext'даги эски 3.5.3 jar'лар ҳам
> тозаланган (фақат `spring-session-core-4.0.0.jar` қолди).

## Ҳозирги ҳолат

- Барча 6 контейнер ишлаяпти: `nginx`(8080), `app1`, `app2`, `hazelcast-1/2`,
  `postgres`(healthy).
- curl билан тўлиқ синалди: логин → `302 /` → дашборд; `/api/ping` app1↔app2
  алмашса ҳам `clicks` ва `sessionId` сақланади; `hazelcast-1`'ни ўчириб
  синалганда ҳам сессия тирик (backup-count=1).

## Қандай ишга тушириш / синаш

```bash
# ko'tarish (agar to'xtagan bo'lsa)
docker compose up -d
docker compose ps

# brauzer: http://localhost:8080  (admin / admin123)
# Yangi inkognito oynada oching (eski cookie muammosidan qochish uchun).

# Session clustering'ni sinash:
docker compose stop app1      # login/hisob saqlanadi
docker compose stop hazelcast-1  # backup tufayli sessiya tirik

# loglar
docker compose logs -f app1 app2
```

Агар jar/config ўзгартирилса — олдин хостда билди шарт:
```bash
mvn -f app/pom.xml clean package -DskipTests
docker compose up --build -d app1 app2
```

## ҚОЛГАН ИШ (TODO)

**Ҳужжатлар ҳали эски механизмни ёзади** — уларни янги архитектурага мослаш
керак (фойдаланувчи розилиги сўралган, ҳали қилинмаган):

- **`CLAUDE.md`** — «Технологиялар»да `Spring Session + Hazelcast (client)`
  тавсифи; қоида #2 (spring-session jar'лари серверда, `SessionUpdateEntryProcessor`,
  `PrincipalNameExtractor`) энди тўғри эмас; қоида #6 «CSRF token сессияда» —
  ҳали тўғри, лекин `/error` permitAll ва tracking-modes'ни эслатиш фойдали;
  `config/SessionConfig.java` изоҳи (`@EnableHazelcastHttpSession`) янгиланиши керак.
- **`ARCHITECTURE.md`** — сессия механизми (EntryProcessor, PrincipalNameExtractor,
  hz-ext'даги spring-session-hazelcast) энди `MapSessionRepository` + BINARY format
  билан алмашган; шуни ёзиш керак.
- **`TROUBLESHOOTING.md`** — янги тузоқлар қўшиш мумкин: (а) Boot 4'да
  spring-session-hazelcast йўқлиги; (б) favicon/`/error` → CSRF 403; (в) nginx
  `$http_host` порт масаласи; (г) Postgres 18 volume mount.

Ишни давом эттирганда: аввал браузерда логин ишлашини тасдиқланг, кейин
юқоридаги ҳужжатларни янгиланг.
