package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Илова кириш нуқтаси (Spring Boot).
 *
 * <p>Бу {@code app.jar} nginx ортида иккита бир хил нусха ({@code app1},
 * {@code app2}) сифатида ишлайди. HTTP-сессия ҳеч бир нусханинг локал хотирасида
 * эмас, ташқи Hazelcast кластерида сақланади — шунинг учун сўров қайси нусхага
 * тушишидан қатъи назар фойдаланувчи логин ҳолатда қолади. Сессия механизми
 * {@link com.example.demo.config.SessionConfig}'да.
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
