# Архитектура

## Умумий кўриниш

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
                          │ postgres:18  │  ← фақат DATA (app_users)
                          └──────────────┘
```

Ҳар бир қатлам битта аниқ вазифани бажаради: nginx — трафикни тақсимлайди, app —
бизнес-логика ва HTML рендер, Hazelcast — умумий сессия сақлаш, Postgres — доимий
маълумот.

## Нима учун сессия йўқолмайди

Оддий stateful иловада сессия Tomcat'нинг локал хотирасида ётади. 2 та нусха
бўлганда:

```
Browser → nginx → app1 : /login       →  сессия app1 хотирасида
Browser → nginx → app2 : /dashboard    →  app2 бу сессияни билмайди → LOGOUT
```

Ечим — сессияни ташқарига чиқариш. `SessionConfig` (`@EnableSpringHttpSession`)
Spring Session'нинг `MapSessionRepository`'сини Hazelcast client'нинг тақсимланган
`IMap`'ига боғлайди. Ҳар икки app ҳам **client режим**да айнан бир кластерга
уланади, шунинг учун сессия умумий бўлади ва хоҳлаган нусха уни ўқий олади.

## Сессия механизми: нима учун native Boot 4

Аввалги ёндашув `spring-session-hazelcast` (`@EnableHazelcastHttpSession`) эди.
Аммо Spring Boot 4 (Spring Session 4.0) бу модулни BOM'дан олиб ташлади — энди
фақат core/redis/jdbc қолган. Шунинг учун эски механизм Boot 4'да умуман
қурилмайди.

Танланган ечим (native Boot 4): `spring-session-core`'даги умумий
`MapSessionRepository`'ни Hazelcast client `IMap` устига қўямиз. `IMap` distributed
бўлгани учун сессия барибир ташқи, умумий store'да қолади — премиса ўзгармайди,
фақат уланиш йўли ўзгарди.

Камчилиги (демо учун аҳамиятсиз): native `IMap` get/put ишлатилгани учун Spring
Session'нинг EntryProcessor delta-update'и ва principal-name индекси йўқолади —
битта сессияни бир вақтда икки сўров янгиласа, охиргиси ютади.

## Логин оқими (қадам-бақадам)

1. Браузер `GET /login` — nginx уни, масалан, app1'га юборади. `PageController.login`
   `login.jte`ни рендер қилади (CSRF token билан).
2. Браузер `POST /login` (username/password) — Spring Security `AppUserDetailsService`
   орқали фойдаланувчини Postgres'дан текширади, BCrypt билан парольни солиштиради.
3. Муваффақиятли бўлса: сессия яратилади, `SecurityContext` унга жойланади ва
   `MapSessionRepository` орқали **Hazelcast IMap'га** ёзилади. Браузерга `SESSION`
   cookie қайтарилади. Session fixation ҳимояси session ID'ни алмаштиради — янги ID
   кластерга ёзилади.
4. Кейинги ҳар бир сўровда браузер `SESSION` cookie'ни юборади. nginx сўровни app1'га
   ҳам, app2'га ҳам бериши мумкин — иккиси ҳам сессияни Hazelcast'дан топади.

## Cookie ва сессия боғланиши

`SESSION` cookie'нинг қиймати — Spring Session сессия ID'си (base64). Иккала app ҳам
бир хил cookie номи (`SESSION`) ва бир хил Hazelcast map (`spring:session:sessions`)
ишлатади, шунинг учун бир app ясаган сессияни иккинчиси ўша ID бўйича топади. Cookie
битта домен (nginx фронти) остида бўлгани учун браузер уни иккала нусхага ҳам юборади.

> Эслатма: сессия фақат cookie орқали кузатилади
> (`server.servlet.session.tracking-modes: cookie`). Busiz Tomcat cookie'сиз
> сўровларда URL'га `;SESSION=...` қўшиб юборарди — бу чалкашлик ва сессия
> churn'ига олиб келади.

## Бардошлилик (fault tolerance)

- **App нусхаси ўчса:** nginx уни upstream'дан чиқаради, сўровлар қолган нусхага
  боради. Сессия Hazelcast'да бўлгани учун ҳеч нима йўқолмайди.
- **Hazelcast node'и ўчса:** `backup-count=1` туфайли ҳар бир сессиянинг нусхаси
  иккинчи node'да туради; кластер уни асосийга кўтаради. Client `redoOperation`
  ва чексиз reconnect билан созланган (`SessionConfig.java`).

Ягона нозик нуқта: битта Hazelcast node билан ишласангиз, у single point of failure
бўлади. Production'да камида 2-3 node тавсия этилади (шунинг учун demo'да ҳам 2 та).

## Нима учун Hazelcast серверида Spring класслари керак эмас

Сессия map'и `hazelcast.xml`'да `in-memory-format: BINARY` билан созланган. Бунда
сервер сессия қийматини фақат **байт массиви** сифатида сақлайди ва ҳеч қачон
объектга айлантирмайди (deserialize қилмайди). Барча (де)сериализацияни app-client
бажаради. Натижада Hazelcast **node**'ида `MapSession` ёки spring-security
класслари бўлиши шарт эмас — node тоза Hazelcast образи бўла олади.

> Эслатма: `build.gradle`'даги `copyHzLibs` (Sync) task `spring-session-core`'ни
> `app/hz-ext/`'га кўчиради ва `Dockerfile.hazelcast` уни node'га қўшади. Бу эски
> (OBJECT-format) ёндашувдан қолган зарарсиз қолдиқ — BINARY'да ишлатилмайди.
> Тозаламоқчи бўлсангиз, `build.gradle`'даги `copyHzLibs` task'ини (ва уни чақирувчи
> `assemble.dependsOn 'copyHzLibs'` қаторини) ҳамда `Dockerfile.hazelcast`'даги
> `COPY app/hz-ext/*.jar` қаторини олиб ташлаш кифоя.

## Компонентлар жадвали

| Компонент      | Технология                | Вазифа                                  |
|----------------|---------------------------|-----------------------------------------|
| Load balancer  | nginx (least_conn)        | Трафикни 2 app орасида тақсимлаш         |
| App x2         | Spring Boot 4, JTE, Alpine| Бизнес-логика, HTML рендер, auth        |
| Сессия store   | Hazelcast cluster (x2)    | Умумий, бардошли сессия сақлаш           |
| Маълумот       | PostgreSQL 18             | Фойдаланувчилар (доимий data)            |
| Auth           | Spring Security           | Form login, BCrypt, CSRF, session fix.  |

## Масштаблаш

App нусхаларини истаганча кўпайтириш мумкин — ҳаммаси бир хил `application.yml`
билан ишлайди, фақат `INSTANCE_NAME` фарқ қилади. `docker-compose.yml`'га `app3`,
`app4`... қўшиб, nginx `upstream`'га уларни киритинг. Hazelcast node'ларини ҳам
шу тарзда кўпайтириб, `backup-count`'ни ошириш мумкин.
