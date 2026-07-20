# Муаммоларни ҳал қилиш (Troubleshooting)

## Ҳали ҳам logout бўляпман / сессия ҳар сўровда алмашяпти

Эҳтимол сессия Hazelcast'га ёзилмаяпти.

- `SessionConfig`'да `@EnableSpringHttpSession` ва `MapSessionRepository` bean'и
  борлигини текширинг (native Boot 4 механизми). Эски `spring.session.store-type`
  проперти энди ишлатилмайди — уни қидирманг.
- `application.yml`'даги `hazelcast.session.map-name` `hazelcast.xml`'даги map номи
  (`spring:session:sessions`) билан айнан бир хил бўлсин.
- App логларида Hazelcast client уланганини кўринг:
  `docker compose logs app1 | grep -i hazelcast` — "CLIENT_CONNECTED" бўлиши керак.
- Иккала app ҳам бир хил `HZ_CLUSTER` (cluster-name) ва бир хил cookie номи
  (`SESSION`) ишлатаётганини текширинг.

## Логиндан кейин «This site can't be reached» / URL'дан порт йўқолади

nginx `Host` header'ни нотўғри узатса, Spring redirect'ни портсиз (`localhost/login`)
қуради ва браузер `localhost:80`'га уриниб хато беради.

- `nginx/nginx.conf`'да `proxy_set_header Host $http_host;` турганини текширинг
  (`$host` эмас — у портни тушириб юборади).
- App томонда `server.forward-headers-strategy: framework` ёқилган бўлсин, шунда
  Spring `X-Forwarded-*` header'ларга ишониб тўғри URL қуради.

## POST/AJAX сўровда 403 Forbidden (CSRF) — favicon/`/error` тузоғи

- AJAX header'ига CSRF token қўшилганини текширинг
  (`dashboard.jte`'даги `pinger()`га қаранг): `X-CSRF-TOKEN` header'и керак.
- Форма POST'ларида яширин CSRF input борлигини текширинг.
- **Логинда 403 такрорланса:** `SecurityConfig`'да `/error` `permitAll` эканини
  текширинг. Акс ҳолда браузер параллел юборадиган `/favicon.ico` (404 → `/error`)
  ҳимояланиб, Spring рақобатчи сессия яратади — форма token'и бир сессияда, браузер
  cookie'си бошқасида қолиб «Invalid CSRF» чиқаверади.

## App Hazelcast'га улана олмаяпти

```
com.hazelcast.client.exception... Unable to connect to any cluster
```

- `HZ_ADDRESSES` тўғрими? (`hazelcast-1:5701,hazelcast-2:5701`).
- `hazelcast.xml`'даги `<cluster-name>` app'нинг `HZ_CLUSTER` қиймати билан бир хилми?
- Hazelcast node'лар бир-бирини топдими? `docker compose logs hazelcast-1` — "Members
  {size:2}" кўриниши керак. Топмаса — `hazelcast.xml`'даги `<tcp-ip>` member номлари
  compose'даги service номлари билан мос эканини текширинг.

## Boot 4'да `spring-session-hazelcast` топилмаяпти (build хатоси)

```
Could not resolve org.springframework.session:spring-session-hazelcast
# ёки: cannot find symbol @EnableHazelcastHttpSession
```

Бу — кутилган ҳол: Spring Boot 4 (Spring Session 4.0) `spring-session-hazelcast`
модулини BOM'дан олиб ташлаган. Эски механизмга қайтманг. `build.gradle`'да
`spring-session-core` турсин, `SessionConfig` эса `@EnableSpringHttpSession` +
`MapSessionRepository` ишлатсин (`ARCHITECTURE.md`'даги «Сессия механизми»).

## Hazelcast node логида `ClassNotFoundException`

```
java.lang.ClassNotFoundException: org.springframework.session...
```

Ҳозирги (BINARY) созламада бу **чиқмаслиги** керак: node сессияни фақат байт
сифатида сақлайди, deserialize қилмайди, шунинг учун Spring класслари унга керак
эмас. Агар шу хато чиқса:

- `hazelcast.xml`'даги сессия map'и `in-memory-format` `BINARY` эканини текширинг
  (кимдир `OBJECT`'га ўзгартирган бўлиши мумкин — OBJECT node'да класслар талаб қилади).
- Сессия map'ига EntryProcessor/индекс/predicate қўшилмаганини текширинг — булар ҳам
  node томонда объектни талаб қилади.

## Версия номувофиқлиги (client/server)

```
Server version ... does not support ... client protocol
```

- `Dockerfile.hazelcast`'даги `hazelcast/hazelcast:5.5.0` тегини app'даги client
  версияга мосланг:
  `./gradlew :app:dependencies --configuration runtimeClasspath | grep -i hazelcast`.

## JTE: старт/рендер хатоси ("No template found" ёки "param mismatch")

- Controller'даги `model.addAttribute(...)` калитлари шаблондаги `@param` номлари ва
  турлари билан аниқ мос келиши керак.
- Precompiled режим ишлатилаётган бўлса (`gg.jte.use-precompiled-templates=true`),
  `.jte` ўзгартиргандан кейин app'ни қайта қуринг (`./gradlew :app:build`), чунки
  шаблонлар build вақтида компиляция қилинади.
- JTE `jte-spring-boot-starter-3` версиясини Spring Boot 4 билан мослигини
  https://mvnrepository.com/artifact/gg.jte дан текширинг.

## Postgres'га улана олмаяпти / маълумот сақланмаяпти

- `postgres` service healthy бўлгунча app кутади (`depends_on ... condition:
  service_healthy`). Логларда `pg_isready` статусини кўринг.
- `DB_URL`, `DB_USER`, `DB_PASSWORD` compose'даги postgres environment билан
  мос эканини текширинг.
- **Postgres 18 volume:** `docker-compose.yml`'да mount `/var/lib/postgresql`
  (эски `/var/lib/postgresql/data` эмас). Postgres 18 маълумотни major-версия
  папкасида сақлайди; нотўғри mount'да маълумот ҳар restart'да йўқолиши мумкин.

## Тоза ҳолатдан қайта бошлаш

```bash
docker compose down -v      # -v: postgres volume'ни ҳам ўчиради
./gradlew :app:clean :app:build
docker compose up --build
```

> Маслаҳат: браузерда эски `SESSION` cookie қолиб кетган бўлса, янги инкогнито
> ойнада синаб кўринг — эски cookie сабабли гоҳида «Invalid CSRF» такрорланади.
