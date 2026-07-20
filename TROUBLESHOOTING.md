# Муаммоларни ҳал қилиш (Troubleshooting)

## Ҳали ҳам logout бўляпман / сессия ҳар сўровда алмашяпти

Эҳтимол сессия Hazelcast'га ёзилмаяпти.

- `application.yml`'да `spring.session.store-type: hazelcast` турганини текширинг.
- App логларида Hazelcast client уланганини кўринг:
  `docker compose logs app1 | grep -i hazelcast` — "CLIENT_CONNECTED" бўлиши керак.
- Иккала app ҳам бир хил `HZ_CLUSTER` (cluster-name) ва бир хил cookie номи
  (`SESSION`) ишлатаётганини текширинг.

## App Hazelcast'га улана олмаяпти

```
com.hazelcast.client.exception... Unable to connect to any cluster
```

- `HZ_ADDRESSES` тўғрими? (`hazelcast-1:5701,hazelcast-2:5701`).
- `hazelcast.xml`'даги `<cluster-name>` app'нинг `HZ_CLUSTER` қиймати билан бир хилми?
- Hazelcast node'лар бир-бирини топдими? `docker compose logs hazelcast-1` — "Members
  {size:2}" кўриниши керак. Топмаса — `hazelcast.xml`'даги `<tcp-ip>` member номлари
  compose'даги service номлари билан мос эканини текширинг.

## ClassNotFoundException Hazelcast node логида

```
java.lang.ClassNotFoundException: org.springframework.session...
```

Бу энг кўп учрайдиган хато: Spring Session jar'лари node classpath'ида йўқ.

- `mvn -f app/pom.xml clean package` ни ишга туширдингизми? У `app/hz-ext/` ичига
  jar'ларни қўяди. Текширинг: `ls app/hz-ext/` — иккита jar бўлиши керак.
- `docker compose build hazelcast-1 hazelcast-2` билан образни қайта қуринг.
- Jar версияси app'никига мос эканини текширинг:
  `mvn -f app/pom.xml dependency:tree | grep spring-session`.

## Версия номувофиқлиги (client/server)

```
Server version ... does not support ... client protocol
```

- `Dockerfile.hazelcast`'даги `hazelcast/hazelcast:5.5.0` тегини app'даги client
  версияга мосланг: `mvn -f app/pom.xml dependency:tree | grep hazelcast`.

## Spring Boot 4 ёки JTE starter топилмаяпти (403/404 Maven)

- `pom.xml`'даги Spring Boot версиясини (`4.0.0`) мавжуд `4.0.x` патчга янгиланг.
- JTE `jte-spring-boot-starter-3` версиясини Spring Boot 4 билан мослигини
  https://mvnrepository.com/artifact/gg.jte дан текширинг.

## POST/AJAX сўровда 403 Forbidden (CSRF)

- AJAX header'ига CSRF token қўшилганини текширинг
  (`dashboard.jte`'даги `pinger()`га қаранг): `X-CSRF-TOKEN` header'и керак.
- Форма POST'ларида яширин CSRF input борлигини текширинг.

## JTE рендер хатоси: "No template found" ёки "param mismatch"

- Controller'даги `model.addAttribute(...)` калитлари шаблондаги `@param` номлари ва
  турлари билан аниқ мос келиши керак.
- Precompiled режим ишлатилаётган бўлса, `.jte` ўзгартиргандан кейин app'ни қайта
  қуринг (`mvn package`), чунки шаблонлар build вақтида компиляция қилинади.

## Postgres'га улана олмаяпти

- `postgres` service healthy бўлгунча app кутади (`depends_on ... condition:
  service_healthy`). Логларда `pg_isready` статусини кўринг.
- `DB_URL`, `DB_USER`, `DB_PASSWORD` compose'даги postgres environment билан
  мос эканини текширинг.

## Тоза ҳолатдан қайта бошлаш

```bash
docker compose down -v      # -v: postgres volume'ни ҳам ўчиради
mvn -f app/pom.xml clean package -DskipTests
docker compose up --build
```
