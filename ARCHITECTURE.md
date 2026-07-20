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

Ечим — сессияни ташқарига чиқариш. `@EnableHazelcastHttpSession` (`SessionConfig.java`)
Spring'нинг `HttpSession`'ини Hazelcast'нинг тақсимланган `IMap`'ига боғлайди. Ҳар
икки app ҳам **client режим**да айнан бир кластерга уланади, шунинг учун сессия
умумий бўлади ва хоҳлаган нусха уни ўқий олади.

## Логин оқими (қадам-бақадам)

1. Браузер `GET /login` — nginx уни, масалан, app1'га юборади. `PageController.login`
   `login.jte`ни рендер қилади (CSRF token билан).
2. Браузер `POST /login` (username/password) — Spring Security `AppUserDetailsService`
   орқали фойдаланувчини Postgres'дан текширади, BCrypt билан парольни солиштиради.
3. Муваффақиятли бўлса: сессия яратилади, `SecurityContext` унга жойланади ва
   **Hazelcast'га** ёзилади. Браузерга `SESSION` cookie қайтарилади. Session fixation
   ҳимояси session ID'ни алмаштиради — янги ID кластерга ёзилади.
4. Кейинги ҳар бир сўровда браузер `SESSION` cookie'ни юборади. nginx сўровни app1'га
   ҳам, app2'га ҳам бериши мумкин — иккиси ҳам сессияни Hazelcast'дан топади.

## Cookie ва сессия боғланиши

`SESSION` cookie'нинг қиймати — Spring Session сессия ID'си (base64). Иккала app ҳам
бир хил cookie номи (`SESSION`) ва бир хил Hazelcast map (`spring:session:sessions`)
ишлатади, шунинг учун бир app ясаган сессияни иккинчиси ўша ID бўйича топади. Cookie
битта домен (nginx фронти) остида бўлгани учун браузер уни иккала нусхага ҳам юборади.

## Бардошлилик (fault tolerance)

- **App нусхаси ўчса:** nginx уни upstream'дан чиқаради, сўровлар қолган нусхага
  боради. Сессия Hazelcast'да бўлгани учун ҳеч нима йўқолмайди.
- **Hazelcast node'и ўчса:** `backup-count=1` туфайли ҳар бир сессиянинг нусхаси
  иккинчи node'да туради; кластер уни асосийга кўтаради. Client `redoOperation`
  ва чексиз reconnect билан созланган (`SessionConfig.java`).

Ягона нозик нуқта: битта Hazelcast node билан ишласангиз, у single point of failure
бўлади. Production'да камида 2-3 node тавсия этилади (шунинг учун demo'да ҳам 2 та).

## Нима учун Spring Session jar'лари Hazelcast серверида

Spring Session сессияни янгилашда серверда ишлайдиган Hazelcast `EntryProcessor`
(`SessionUpdateEntryProcessor`) ва principal индекси учун `PrincipalNameExtractor`
классларини ишлатади, шунингдек `MapSession` қийматини десериализация қилади. Булар
Hazelcast **node**'нинг classpath'ида бўлмаса, сессия ёзилмайди/ўқилмайди. Шунинг учун
`Dockerfile.hazelcast` app билан бир хил версиядаги `spring-session-hazelcast` ва
`spring-session-core` jar'ларини node'га қўшади.

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
