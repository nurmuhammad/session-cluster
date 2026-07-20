package com.smartbox.sessioncluster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Илова кириш нуқтаси (Spring Boot).
 *
 * <p>Бу {@code app.jar} nginx ортида иккита бир хил нусха ({@code app1},
 * {@code app2}) сифатида ишлайди. HTTP-сессия ҳеч бир нусханинг локал хотирасида
 * эмас, ташқи Hazelcast кластерида сақланади — шунинг учун сўров қайси нусхага
 * тушишидан қатъи назар фойдаланувчи логин ҳолатда қолади. Сессия механизми
 * {@link com.smartbox.sessioncluster.config.SessionConfig}'да.
 */
@SpringBootApplication
public class SessionClusterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionClusterApplication.class, args);
    }
}
