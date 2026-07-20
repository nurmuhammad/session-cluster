package com.example.demo.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import java.time.Duration;
import java.util.Arrays;

/**
 * Sessiyani ALOHIDA ishlab turgan Hazelcast serveriga (cluster'ga) yozadi.
 *
 * MUHIM: Spring Boot 4 (Spring Session 4.0) spring-session-hazelcast modulini
 * olib tashladi. Shuning uchun bu yerda spring-session-core'ning umumiy
 * {@link MapSessionRepository}'sidan foydalanamiz: u har qanday {@link java.util.Map}
 * ustida ishlaydi, Hazelcast client'ning IMap'i esa aynan Map'dir. Natijada
 * sessiya baribir tashqi, umumiy Hazelcast store'da yotadi — arxitektura o'zgarmaydi.
 *
 * embedded emas, CLIENT rejim ishlatiladi: ikkala app.jar ham shu klient orqali
 * bitta Hazelcast cluster'iga ulanadi, shuning uchun sessiya har ikkala instance
 * uchun umumiy bo'ladi.
 */
@Configuration
@EnableSpringHttpSession
public class SessionConfig {

    /**
     * Hazelcast node manzillari, vergul bilan ajratilgan.
     * Masalan: "hazelcast-1:5701,hazelcast-2:5701"
     */
    @Value("${hazelcast.addresses:127.0.0.1:5701}")
    private String hazelcastAddresses;

    @Value("${hazelcast.cluster-name:spring-session-cluster}")
    private String clusterName;

    /** Sessiyalar saqlanadigan Hazelcast map nomi (hazelcast.xml bilan bir xil). */
    @Value("${hazelcast.session.map-name:spring:session:sessions}")
    private String sessionMapName;

    /** Sessiya harakatsizlik timeout'i (daqiqa). */
    @Value("${hazelcast.session.timeout-minutes:30}")
    private long sessionTimeoutMinutes;

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);

        String[] addresses = Arrays.stream(hazelcastAddresses.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        clientConfig.getNetworkConfig().addAddress(addresses);

        // Bitta node o'chsa, klient boshqa node'ga qayta ulanaveradi
        clientConfig.getNetworkConfig().setRedoOperation(true);
        clientConfig.getConnectionStrategyConfig()
                .getConnectionRetryConfig()
                .setClusterConnectTimeoutMillis(Long.MAX_VALUE);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Spring Session'ning SessionRepository'si Hazelcast IMap ustida.
     * IMap distributed bo'lgani uchun sessiya butun cluster bo'ylab ko'rinadi.
     */
    @Bean
    public SessionRepository<? extends Session> sessionRepository(HazelcastInstance hazelcastInstance) {
        IMap<String, Session> sessions = hazelcastInstance.getMap(sessionMapName);
        MapSessionRepository repository = new MapSessionRepository(sessions);
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(sessionTimeoutMinutes));
        return repository;
    }
}
