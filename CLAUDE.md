# CLAUDE.md

Бу файл Claude Code (ва бошқа AI-агентлар) учун ушбу репозиторийда ишлаш бўйича
йўриқнома. Лойиҳа устида ишлашдан олдин шуни ўқинг.

## Лойиҳа нима қилади

Spring Boot 4 веб-илова икки нусхада (`app1`, `app2`) load balancer (nginx) ортида
ишлайди. HTTP-сессия **алоҳида Hazelcast кластерида** сақланади, шунинг учун сўров
қайси app нусхасига тушишидан қатъи назар фойдаланувчи логин ҳолатда қолади
(session clustering). Postgres фақат биsnес-датани (фойдаланувчилар) сақлайди.

Асосий ғоя: сессия ҳеч қайси app нусхасининг **локал хотирасида** эмас, ташқи,
умумий store'да (Hazelcast) ётади.

## Технологиялар

- Java 21, Spring Boot 4 (Web, Security, Data JPA)
- Spring Session + Hazelcast (client режим)
- JTE — сервер томонда HTML рендер қилувчи шаблон двигатели
- AlpineJS — енгил клиент-томон интерактивлик (CDN орқали)
- PostgreSQL 18 — фақат маълумот
- nginx — load balancer (least_conn)
- Docker Compose — бутун стекни кўтаради

## Каталог тузилиши

```
session-cluster/
├── docker-compose.yml          # 2 app + 2 hazelcast + postgres + nginx
├── Dockerfile.hazelcast        # Hazelcast + spring-session jar'lari
├── nginx/nginx.conf            # load balancer
├── hazelcast/hazelcast.xml     # cluster + session map (backup-count=1)
├── README.md                   # ishga tushirish qo'llanmasi
├── ARCHITECTURE.md             # chuqurroq arxitektura izohi
├── TROUBLESHOOTING.md          # tez-tez uchraydigan muammolar
└── app/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/com/example/demo/
        │   ├── DemoApplication.java
        │   ├── config/SessionConfig.java     # Hazelcast CLIENT + @EnableHazelcastHttpSession
        │   ├── config/SecurityConfig.java    # form login + CSRF
        │   ├── web/PageController.java        # dashboard + /api/ping
        │   └── user/                          # JPA entity, repo, UserDetailsService, seeder
        ├── resources/application.yml
        └── jte/                               # layout.jte, login.jte, dashboard.jte
```

## Тез-тез ишлатиладиган буйруқлар

```bash
# App'ni qurish (target/*.jar va app/hz-ext/*.jar hosil bo'ladi)
mvn -f app/pom.xml clean package -DskipTests

# Faqat kompilyatsiya
mvn -f app/pom.xml compile

# Butun stack'ni ko'tarish
docker compose up --build

# Bitta app nusxasini o'chirib sinash (sessiya saqlanishi kerak)
docker compose stop app1

# Hazelcast versiyasini tekshirish (server image'ga moslash uchun)
mvn -f app/pom.xml dependency:tree | grep hazelcast

# Loglar
docker compose logs -f app1 hazelcast-1
```

Илова: `http://localhost:8080` · демо логин: `admin` / `admin123`.

## Муҳим қоидалар ва тузоқлар (буни билиб туринг)

1. **Сессия локал хотирада бўлмаслиги керак.** Ҳар қандай сессияга оид код
   `HttpSession`'дан фойдаланиши шарт — уни Spring Session автоматик Hazelcast'га
   ёзади. Статик `Map`, `@SessionScope` bean, ёки локал кэшда фойдаланувчи ҳолатини
   сақламанг — акс ҳолда кластерда бузилади.

2. **Spring Session jar'лари Hazelcast серверида бўлиши ШАРТ.** Ташқи Hazelcast
   серверда `SessionUpdateEntryProcessor`, `PrincipalNameExtractor`, `MapSession`
   классларини ишлатади. `pom.xml`'даги `maven-dependency-plugin` уларни
   `app/hz-ext/`'га кўчиради, `Dockerfile.hazelcast` эса node'га қўшади. Бу
   механизмни бузманг; версия app билан бир хил бўлиши керак.

3. **Версияга сезгир жойлар.** Spring Boot `4.0.0`, JTE `3.2.1`, Hazelcast server
   образи `5.5.0`. Client (app) Hazelcast версиясини server образига мосланг.

4. **JTE шаблонлари build вақтида компиляция қилинади** (`jte-maven-plugin`нинг
   `generate` goal'и + `gg.jte.use-precompiled-templates=true`). Янги `.jte`
   қўшсангиз, model калитлари шаблондаги `@param`лар билан аниқ мос келиши керак.

5. **AlpineJS'да `@` белгиси JTE билан тўқнашади.** `.jte` ичида Alpine'нинг
   `@click`ини `@@click` деб ёзинг (`@@` → литерал `@`).

6. **CSRF ёқилган.** POST/AJAX сўровларда CSRF token'ни header'га қўшинг
   (`dashboard.jte`'даги `pinger()`га қаранг). Token сессияда — Hazelcast орқали умумий.

7. **Sticky session КЕРАК ЭМАС.** Сессия ташқарида бўлгани учун nginx'да
   round-robin ҳам, least_conn ҳам ишлайверади. Stickiness қўшманг.

## Конвенциялар

- Ҳар бир app нусхаси айнан бир хил `application.yml` билан ишлайди; фарқ фақат
  environment o'zgaruvchиларда (`INSTANCE_NAME`, `HZ_ADDRESSES`, `DB_*`).
- Конфигурация environment o'zгарувчилари орқали бошқарилади (12-factor).
- Yangi endpoint qo'shsangiz, `SecurityConfig`'да ruxsatlar (permitAll/authenticated)ни
  ям yangилашни унутманг.

## Билмаган нарсангизда

- Ишга тушириш: `README.md`
- Архитектура ва оқим: `ARCHITECTURE.md`
- Хатолар: `TROUBLESHOOTING.md`
