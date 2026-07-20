# Spring Boot 4 + JTE + AlpineJS + Hazelcast session-cluster

Иккита `app.jar` load balancer ортида ишлайди, сессия эса **алоҳида Hazelcast
кластерида** сақланади. Шу сабабли фойдаланувчи қайси app нусхасига тушишидан
қатъи назар логин ҳолатда қолади — **logout бўлиб кетмайди**. Postgres фақат
бизнес-маълумот (фойдаланувчилар) учун ишлатилади.

```
                          ┌──────────────┐
                    ┌────▶│    app1      │────┐
  Browser           │     │  (app.jar)   │    │     ┌────────────────────────────┐
    │      ┌────────┤     └──────────────┘    ├────▶│ hazelcast-1 ── hazelcast-2 │  ← СЕССИЯ
    ▼      │ nginx  │     ┌──────────────┐    │     │  (cluster, backup-count=1) │
 [:8080]──▶│least_conn│─▶│    app2      │────┘     └────────────────────────────┘
           └────────┘     │  (app.jar)   │
                          └──────────────┘
                                 │  JPA
                                 ▼
                          ┌──────────────┐
                          │ postgres:18  │  ← фақат DATA
                          └──────────────┘
```

## Нима учун сессия йўқолмайди?

`SessionConfig` сессияни Hazelcast client'нинг тақсимланган `IMap`'ига боғлайди
(`@EnableSpringHttpSession` + `MapSessionRepository`). Иккала app ҳам **client
режим**да битта Hazelcast кластерига уланади, шунинг учун сессия умумий. Натижада
load balancer'да **sticky session шарт эмас** — least_conn ҳам, round-robin ҳам
бемалол ишлайверади.

> Эслатма (Boot 4): Spring Boot 4 `spring-session-hazelcast` модулини олиб
> ташлаган, шунинг учун бу лойиҳа `spring-session-core`'нинг умумий
> `MapSessionRepository`'сини Hazelcast `IMap` устида ишлатади. Тафсилот
> `ARCHITECTURE.md`'да.

## Ишга тушириш

Талаб: Docker + Docker Compose, ҳамда Java 21 (app'ни қуриш учун). Gradle wrapper
репода бор — алоҳида Gradle ўрнатиш шарт эмас.

```bash
# 1) App'ни қурамиз. Бу build/libs/*.jar ва app/hz-ext/*.jar ҳосил қилади.
#    Windows PowerShell'да: .\gradlew.bat :app:build
./gradlew :app:build

# 2) Бутун стекни кўтарамиз (2 app + 2 hazelcast + postgres + nginx)
docker compose up --build
```

Браузерда очинг: **http://localhost:8080**
Демо логин: **admin / admin123**

> Маслаҳат: янги инкогнито ойнада очинг — эски `SESSION` cookie сабабли гоҳида
> «Invalid CSRF» такрорланиши мумкин.

### IntelliJ IDEA'да очиш

Илдиз каталогни (`session-cluster/`) `File → Open` билан очинг — IDEA `settings.gradle`'ни
кўриб Gradle лойиҳа сифатида импорт қилади. Барча Java файл танилиши учун:

- **Project SDK** ва **Gradle JVM** = **JDK 21** бўлсин
  (`Settings → Build Tools → Gradle`, ҳамда `Project Structure → Project`).
- Gradle sync тугагач `app/build/generated-sources/jte` автоматик source root бўлади.

## Сессия сақланишини қандай синаш мумкин?

1. Логин қилинг. Дашбордда «жавоб берган нусха» (app1 ёки app2) кўринади.
2. Саҳифани бир неча марта янгиланг (F5). Load balancer сизни гоҳ app1, гоҳ app2'га
   юборади — лекин сиз **logout бўлмайсиз** ва «Саҳифа очилиши» ҳисоби ўсиб боради
   (у сессияда, яъни Hazelcast'да сақланади).
3. «Ping юбориш» тугмаси (AlpineJS) `/api/ping`'ни чақиради; жавоб берган нусха
   алмашиб турса ҳам «Ping ҳисоби» сақланиб қолаверади.
4. **Бардошлиликни синаш:** битта app'ни ўчиринг —
   `docker compose stop app1` — барибир ишлайди, сессия жойида.
   Битта Hazelcast node'ни ўчиринг — `docker compose stop hazelcast-1` —
   `backup-count=1` туфайли сессиялар иккинчи node'дан тикланади.

## Нима учун Hazelcast серверида Spring jar'лари керак эмас?

Сессия map'и `hazelcast.xml`'да `in-memory-format: BINARY` билан созланган — сервер
сессияни фақат **байт** сифатида сақлайди ва ҳеч қачон deserialize қилмайди. Барча
(де)сериализацияни app-client бажаради. Шунинг учун Hazelcast node'ида `MapSession`
ёки spring-security класслари **бўлиши шарт эмас**.

> Эслатма: `build.gradle`'даги `copyHzLibs` task `spring-session-core`'ни `app/hz-ext/`'га
> кўчиради ва `Dockerfile.hazelcast` уни node'га қўшади — бу эски (OBJECT-format) ёндашувдан
> қолган зарарсиз қолдиқ. BINARY'да ишлатилмайди; хоҳласангиз иккала жойдан ҳам
> олиб ташлашингиз мумкин.

## Версияларга эътибор беринг (build олдидан текширинг)

Бу лойиҳа қуйидагиларни ишлатади; муҳитингизга қараб мослашингиз керак бўлиши мумкин:

- **Spring Boot** `4.0.0` — янгироқ патч чиққан бўлса (`4.0.x`), `build.gradle`'даги
  plugin версиясини янгиланг. (`spring-session-core` версияси шу BOM'дан келади.)
- **JTE** `jte-spring-boot-starter-3` `3.2.1` — Spring Boot 4 билан мос starter
  версиясини https://mvnrepository.com/artifact/gg.jte дан текширинг.
- **Hazelcast server образи** `hazelcast/hazelcast:5.5.0` — client (app) версиясига
  мосланг:  `./gradlew :app:dependencies --configuration runtimeClasspath | grep -i hazelcast`
- **Postgres** `postgres:18`.

## Ҳужжатлар

- `README.md` — шу файл: ишга тушириш қўлланмаси.
- `CLAUDE.md` — AI-агентлар (Claude Code) учун лойиҳа йўриқномаси.
- `ARCHITECTURE.md` — архитектура, логин оқими ва бардошлилик изоҳи.
- `TROUBLESHOOTING.md` — тез-тез учрайдиган муаммолар ва ечимлар.

## Файллар тузилиши

```
session-cluster/
├── settings.gradle · gradlew · gradlew.bat · gradle/wrapper/   # Gradle (9.1.0)
├── docker-compose.yml          # 2 app + 2 hazelcast + postgres + nginx
├── Dockerfile.hazelcast        # Hazelcast node (BINARY session map)
├── nginx/nginx.conf            # load balancer (least_conn)
├── hazelcast/hazelcast.xml     # cluster + session map (BINARY, backup-count=1)
├── README.md · CLAUDE.md · ARCHITECTURE.md · TROUBLESHOOTING.md
└── app/
    ├── build.gradle            # Spring Boot 4 + JTE + copyHzLibs
    ├── Dockerfile
    └── src/main/
        ├── java/com/example/demo/
        │   ├── config/SessionConfig.java     # Hazelcast CLIENT + @EnableSpringHttpSession
        │   ├── config/SecurityConfig.java    # form login, CSRF
        │   ├── web/PageController.java        # dashboard + /api/ping
        │   └── user/                          # JPA entity, repo, UserDetailsService, seeder
        ├── resources/application.yml
        └── jte/                               # layout.jte, login.jte, dashboard.jte
```

## Production учун маслаҳатлар

- HTTPS ортида `server.servlet.session.cookie.secure: true` қўйинг.
- Hazelcast'ни камида 2-3 node қилиб, `backup-count`'ни оширинг.
- App нусхаларини хоҳлаганча кўпайтиринг — ҳаммаси бир хил `application.yml`
  билан ишлайди, фақат `INSTANCE_NAME` фарқ қилади.
