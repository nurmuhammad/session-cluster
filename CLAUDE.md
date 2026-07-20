# CLAUDE.md

Бу файл Claude Code (ва бошқа AI-агентлар) учун ушбу репозиторийда ишлаш бўйича
йўриқнома. Лойиҳа устида ишлашдан олдин шуни ўқинг.

## Лойиҳа нима қилади

Spring Boot 4 веб-илова икки нусхада (`app1`, `app2`) load balancer (nginx) ортида
ишлайди. HTTP-сессия **алоҳида Hazelcast кластерида** сақланади, шунинг учун сўров
қайси app нусхасига тушишидан қатъи назар фойдаланувчи логин ҳолатда қолади
(session clustering). Postgres фақат бизнес-датани (фойдаланувчилар) сақлайди.

Асосий ғоя: сессия ҳеч қайси app нусхасининг **локал хотирасида** эмас, ташқи,
умумий store'да (Hazelcast) ётади.

## Технологиялар

- Java 21, Spring Boot 4 (Web, Security, Data JPA)
- Spring Session (`spring-session-core`) — сессияни Hazelcast client `IMap`'ига
  `MapSessionRepository` орқали ёзади (native Boot 4 усули; тафсилот `ARCHITECTURE.md`'да)
- Hazelcast 5.5 — ташқи сессия store (client режим)
- JTE — сервер томонда HTML рендер қилувчи шаблон двигатели
- AlpineJS — енгил клиент-томон интерактивлик (CDN орқали)
- PostgreSQL 18 — фақат маълумот
- nginx — load balancer (least_conn)
- Docker Compose — бутун стекни кўтаради

## Каталог тузилиши

```
session-cluster/
├── settings.gradle             # Gradle root (include 'app')
├── gradlew, gradlew.bat        # Gradle wrapper — алоҳида Gradle керак эмас
├── gradle/wrapper/             # wrapper jar + properties (Gradle 9.1.0)
├── docker-compose.yml          # 2 app + 2 hazelcast + postgres + nginx
├── Dockerfile.hazelcast        # Hazelcast node (BINARY session map)
├── nginx/nginx.conf            # load balancer
├── hazelcast/hazelcast.xml     # cluster + session map (BINARY, backup-count=1)
├── README.md                   # ишга тушириш қўлланмаси
├── ARCHITECTURE.md             # чуқурроқ архитектура изоҳи
├── TROUBLESHOOTING.md          # тез-тез учрайдиган муаммолар
└── app/
    ├── build.gradle            # Spring Boot 4 + JTE + copyHzLibs (асосий конфиг)
    ├── hz-ext/                 # copyHzLibs яратади (commit қилинмайди)
    ├── Dockerfile
    └── src/main/
        ├── java/com/smartbox/sessioncluster/
        │   ├── DemoApplication.java
        │   ├── config/SessionConfig.java     # Hazelcast CLIENT + @EnableSpringHttpSession
        │   ├── config/SecurityConfig.java    # form login + CSRF
        │   ├── web/PageController.java        # dashboard + /api/ping
        │   └── user/                          # JPA entity, repo, UserDetailsService, seeder
        ├── resources/application.yml
        └── jte/                               # layout.jte, login.jte, dashboard.jte
```

## Тез-тез ишлатиладиган буйруқлар

```bash
# App'ни қуриш (build/libs/*.jar ва app/hz-ext/*.jar ҳосил бўлади)
# Windows PowerShell'да: .\gradlew.bat :app:build
./gradlew :app:build

# Фақат компиляция
./gradlew :app:compileJava

# Бутун стекни кўтариш
docker compose up --build

# Битта app нусхасини ўчириб синаш (сессия сақланиши керак)
docker compose stop app1

# Hazelcast версиясини текшириш (server образига мослаш учун)
./gradlew :app:dependencies --configuration runtimeClasspath | grep -i hazelcast

# Логлар
docker compose logs -f app1 hazelcast-1
```

Илова: `http://localhost:8080` · демо логин: `admin` / `admin123`.

## Муҳим қоидалар ва тузоқлар (буни билиб туринг)

1. **Сессия локал хотирада бўлмаслиги керак.** Ҳар қандай сессияга оид код
   `HttpSession`'дан фойдаланиши шарт — уни Spring Session автоматик Hazelcast'га
   ёзади. Статик `Map`, `@SessionScope` bean, ёки локал кэшда фойдаланувчи ҳолатини
   сақламанг — акс ҳолда кластерда бузилади.

2. **Сессия механизми — native Boot 4.** Spring Boot 4 `spring-session-hazelcast`
   модулини олиб ташлаган, шунинг учун `@EnableHazelcastHttpSession`'ни қайта
   қўшманг — у Boot 4'да умуман қурилмайди. Ўрнига `SessionConfig`'да
   `@EnableSpringHttpSession` + `MapSessionRepository` Hazelcast client `IMap`
   устида ишлайди (`ARCHITECTURE.md`'га қаранг).

3. **Hazelcast серверида Spring jar'лари керак ЭМАС.** Сессия map'и
   `hazelcast.xml`'да `in-memory-format: BINARY` — node фақат байт сақлайди,
   deserialize қилмайди. Шу боис node classpath'ида `MapSession` ёки
   spring-security класслари бўлиши шарт эмас. (`build.gradle`'даги `copyHzLibs` (Sync)
   task `spring-session-core`'ни `app/hz-ext/`'га кўчиради — эски OBJECT-format
   ёндашувдан қолган зарарсиз қолдиқ,
   BINARY'да ишлатилмайди.)

4. **Версияга сезгир жойлар.** Spring Boot `4.0.0`, JTE `3.2.1`, Hazelcast server
   образи `5.5.0`, Gradle `9.1.0` (wrapper'да ёзилган). `spring-session-core`
   версияси Boot BOM'дан келади (алоҳида ёзилмайди). Client (app) Hazelcast
   версиясини server образига мосланг.

5. **JTE шаблонлари build вақтида компиляция қилинади** (`gg.jte.gradle` Gradle
   плагини, `jte { generate() }` + `gg.jte.use-precompiled-templates=true`). Янги `.jte`
   қўшсангиз, model калитлари шаблондаги `@param`лар билан аниқ мос келиши керак.

6. **AlpineJS'да `@` белгиси JTE билан тўқнашади.** `.jte` ичида Alpine'нинг
   `@click`ини `@@click` деб ёзинг (`@@` → литерал `@`).

7. **CSRF ёқилган.** POST/AJAX сўровларда CSRF token'ни header'га қўшинг
   (`dashboard.jte`'даги `pinger()`га қаранг). Token сессияда — Hazelcast орқали
   умумий. Шунингдек `/error` `permitAll` бўлиши ва
   `server.servlet.session.tracking-modes: cookie` туриши шарт — акс ҳолда
   favicon/`/error` сабабли рақобатчи сессия яралиб «Invalid CSRF» (403) чиқади
   (`SecurityConfig` JavaDoc'ига қаранг).

8. **Sticky session КЕРАК ЭМАС.** Сессия ташқарида бўлгани учун nginx'да
   round-robin ҳам, least_conn ҳам ишлайверади. Stickiness қўшманг.

## Конвенциялар

- Ҳар бир app нусхаси айнан бир хил `application.yml` билан ишлайди; фарқ фақат
  environment ўзгарувчиларда (`INSTANCE_NAME`, `HZ_ADDRESSES`, `DB_*`).
- Конфигурация environment ўзгарувчилари орқали бошқарилади (12-factor).
- Янги endpoint қўшсангиз, `SecurityConfig`'да рухсатлар
  (permitAll/authenticated)ни ҳам янгилашни унутманг.
- Сессияга ёзадиган код фақат `HttpSession` ишлатсин (қоида №1).

## Билмаган нарсангизда

- Ишга тушириш: `README.md`
- Архитектура ва оқим: `ARCHITECTURE.md`
- Хатолар: `TROUBLESHOOTING.md`
