# Spring Boot 4 + JTE + AlpineJS + Hazelcast session-cluster

Ikkita `app.jar` load balancer ortida ishlaydi, sessiya esa **alohida Hazelcast
cluster'ida** saqlanadi. Shu sababli foydalanuvchi qaysi app instance'ga tushishidan
qat'i nazar login holatida qoladi — **logout bo'lib ketmaydi**. Postgres faqat
biznes-ma'lumot (foydalanuvchilar) uchun ishlatiladi.

```
                          ┌──────────────┐
                    ┌────▶│    app1      │────┐
  Browser           │     │  (app.jar)   │    │     ┌───────────────────────┐
    │      ┌────────┤     └──────────────┘    ├────▶│  hazelcast-1 ─ hazelcast-2 │  ← SESSIYA
    ▼      │ nginx  │     ┌──────────────┐    │     │  (cluster, backup=1)      │
 [8080]───▶│least_conn│──▶│    app2      │────┘     └───────────────────────┘
           └────────┘     │  (app.jar)   │
                          └──────────────┘
                                 │
                                 ▼
                          ┌──────────────┐
                          │ postgres:18  │  ← faqat DATA
                          └──────────────┘
```

## Nima uchun sessiya yo'qolmaydi?

`spring-session-hazelcast` `HttpSession`'ni Hazelcast'ning taqsimlangan map'iga yozadi
(`SessionConfig.java` dagi `@EnableHazelcastHttpSession`). Ikkala app ham **client
rejim**da bitta Hazelcast cluster'iga ulanadi, shuning uchun sessiya umumiy. Natijada
load balancer'da **sticky session shart emas** — least_conn ham, round-robin ham
bemalol ishlaydi.

## Ishga tushirish

Talab: Docker + Docker Compose, hamda Java 21 + Maven (app'ni qurish uchun).

```bash
# 1) App'ni quramiz. Bu target/*.jar va app/hz-ext/*.jar hosil qiladi.
mvn -f app/pom.xml clean package -DskipTests

# 2) Butun stack'ni ko'taramiz (2 app + 2 hazelcast + postgres + nginx)
docker compose up --build
```

Brauzerda oching: **http://localhost:8080**
Demo login: **admin / admin123**

## Sessiya saqlanishini qanday sinash mumkin?

1. Login qiling. Dashboard'da "javob bergan instance" (app1 yoki app2) ko'rinadi.
2. Sahifani bir necha marta yangilang (F5). Load balancer sizni goh app1, goh app2 ga
   yuboradi — lekin siz **logout bo'lmaysiz** va "Sahifa ochilishi" hisobi o'sib boradi
   (u sessiyada, ya'ni Hazelcast'da saqlanadi).
3. "Ping yuborish" tugmasi (AlpineJS) `/api/ping` ni chaqiradi; javob bergan instance
   almashib tursa ham "Ping hisobi" saqlanib qolaveradi.
4. **Bardoshlilikni sinash:** bitta app'ni o'chiring —
   `docker compose stop app1` — baribir ishlaydi, sessiya joyida.
   Bitta Hazelcast node'ni o'chiring — `docker compose stop hazelcast-1` —
   `backup-count=1` tufayli sessiyalar ikkinchi node'dan tiklanadi.

## Muhim: nega Spring Session jar'lari Hazelcast serverida?

Tashqi (client-server) Hazelcast'da Spring Session server tomonida o'z sinflarini
ishlatadi (`SessionUpdateEntryProcessor`, `PrincipalNameExtractor`, `MapSession`).
Shu sabab ular Hazelcast node classpath'ida bo'lishi shart. `pom.xml`'dagi
`maven-dependency-plugin` bu jar'larni `app/hz-ext/` ga nusxalaydi va
`Dockerfile.hazelcast` ularni node'ga qo'shadi — versiya app bilan aynan bir xil bo'ladi.

## Versiyaga e'tibor bering (build oldidan tekshiring)

Bu loyiha quyidagilarni ishlatadi; muhitingizga qarab moslashingiz kerak bo'lishi mumkin:

- **Spring Boot** `4.0.0` — agar yangiroq patch chiqqan bo'lsa (`4.0.x`),
  `pom.xml`'dagi `<version>` ni yangilang.
- **JTE** `jte-spring-boot-starter-3` `3.2.1` — Spring Boot 4 bilan mos starter
  versiyasini https://mvnrepository.com/artifact/gg.jte dan tekshiring.
- **Hazelcast server image** `hazelcast/hazelcast:5.5.0` — client (app) versiyasiga
  moslang:  `mvn -f app/pom.xml dependency:tree | grep hazelcast`
- **Postgres** `postgres:18`.

> Eslatma: bu loyiha Maven Central'ga chiqa olmaydigan sandbox'da yozilgani uchun
> u yerda kompilyatsiya qilinmagan. Kod va konfiguratsiya qo'lda tekshirilgan;
> yuqoridagi versiyalarni o'z muhitingizda bir marta tasdiqlab oling.

## Hujjatlar

- `README.md` — shu fayl: ishga tushirish qo'llanmasi.
- `CLAUDE.md` — AI-agentlar (Claude Code) uchun loyiha yo'riqnomasi.
- `ARCHITECTURE.md` — arxitektura, login oqimi va bardoshlilik izohi.
- `TROUBLESHOOTING.md` — tez-tez uchraydigan muammolar va yechimlar.

## Fayllar tuzilishi

```
session-cluster/
├── docker-compose.yml          # 2 app + 2 hazelcast + postgres + nginx
├── Dockerfile.hazelcast        # Hazelcast + spring-session jar'lari
├── nginx/nginx.conf            # load balancer (least_conn)
├── hazelcast/hazelcast.xml     # cluster + session map (backup-count=1)
├── README.md · CLAUDE.md · ARCHITECTURE.md · TROUBLESHOOTING.md
└── app/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/com/example/demo/
        │   ├── config/SessionConfig.java     # Hazelcast CLIENT + @EnableHazelcastHttpSession
        │   ├── config/SecurityConfig.java    # form login, CSRF
        │   ├── web/PageController.java        # dashboard + /api/ping
        │   └── user/                          # JPA entity, repo, UserDetailsService, seeder
        ├── resources/application.yml
        └── jte/                               # layout.jte, login.jte, dashboard.jte
```

## Production uchun maslahatlar

- HTTPS ortida `server.servlet.session.cookie.secure: true` qo'ying.
- Hazelcast'ni kamida 2-3 node qilib, `backup-count` ni oshiring.
- App instance'larini xohlagancha ko'paytiring — hammasi bir xil `application.yml`
  bilan ishlaydi, faqat `INSTANCE_NAME` farq qiladi.
