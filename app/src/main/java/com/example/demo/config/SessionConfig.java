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
 * HTTP-сессияни ташқи Hazelcast кластерига боғлайди (native Spring Boot 4 усули).
 *
 * <p>Spring Boot 4 (Spring Session 4.0) {@code spring-session-hazelcast}
 * модулини BOM'дан олиб ташлади, шунинг учун эски
 * {@code @EnableHazelcastHttpSession} механизми бу ерда умуман қурилмайди.
 * Ўрнига {@code spring-session-core}'даги умумий {@link MapSessionRepository}'дан
 * фойдаланамиз: у истаган {@link java.util.Map} устида ишлайди, Hazelcast
 * client'нинг {@link IMap}'и эса айнан {@code Map}. {@link IMap} distributed
 * бўлгани учун сессия барибир ташқи, умумий store'да қолади — премиса
 * ўзгармайди, фақат уланиш йўли ўзгарди.
 *
 * <p>embedded эмас, <b>CLIENT</b> режим ишлатилади: иккала {@code app.jar} ҳам
 * шу client орқали битта Hazelcast кластерига уланади, шунинг учун сессия ҳар
 * икки нусха учун умумий.
 *
 * <p>Камчилиги (демо учун аҳамиятсиз): native {@code IMap} get/put ишлатилгани
 * учун Spring Session'нинг EntryProcessor delta-update'и ва principal-name
 * индекси йўқолади — битта сессияни бир вақтда икки сўров янгиласа, охиргиси ютади.
 */
@Configuration
@EnableSpringHttpSession
public class SessionConfig {

    /** Hazelcast node манзиллари, вергул билан ажратилган (масалан {@code hazelcast-1:5701,hazelcast-2:5701}). */
    @Value("${hazelcast.addresses:127.0.0.1:5701}")
    private String hazelcastAddresses;

    @Value("${hazelcast.cluster-name:spring-session-cluster}")
    private String clusterName;

    /** Сессиялар сақланадиган Hazelcast map номи; {@code hazelcast.xml}'даги map номи билан бир хил бўлиши шарт. */
    @Value("${hazelcast.session.map-name:spring:session:sessions}")
    private String sessionMapName;

    /** Сессиянинг ҳаракатсизлик timeout'и (дақиқа). */
    @Value("${hazelcast.session.timeout-minutes:30}")
    private long sessionTimeoutMinutes;

    /**
     * Hazelcast кластерига CLIENT режимда уланувчи {@link HazelcastInstance} яратади.
     *
     * <p>Client битта node ўчганда бошқасига чидамли бўлиши учун созланган:
     * {@code redoOperation} узилган операцияни қайта юборади, {@code clusterConnectTimeout}
     * эса чексиз — шунда кластер вақтинча йўқолса ҳам client узилмай қайта
     * уланишга уринаверади. {@code destroyMethod = "shutdown"} bean йўқолганда
     * client'ни тоза ёпади.
     *
     * @return созланган Hazelcast client instance'и
     */
    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);

        String[] addresses = Arrays.stream(hazelcastAddresses.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        clientConfig.getNetworkConfig().addAddress(addresses);

        // Битта node ўчса, client бошқа node'га қайта уланаверади
        clientConfig.getNetworkConfig().setRedoOperation(true);
        clientConfig.getConnectionStrategyConfig()
                .getConnectionRetryConfig()
                .setClusterConnectTimeoutMillis(Long.MAX_VALUE);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Spring Session'нинг {@link SessionRepository}'сини Hazelcast {@link IMap}
     * устига қуради.
     *
     * <p>{@link MapSessionRepository} сессияларни оддий {@code Map} операциялари
     * (get/put/remove) орқали сақлайди. {@link IMap} distributed бўлгани учун
     * битта нусха ёзган сессияни бошқа нусха ўша {@code sessionId} бўйича топади —
     * session clustering'нинг асоси шу.
     *
     * @param hazelcastInstance CLIENT режимдаги Hazelcast уланиши
     * @return {@code IMap} билан таъминланган сессия репозиторийси
     */
    @Bean
    public SessionRepository<? extends Session> sessionRepository(HazelcastInstance hazelcastInstance) {
        IMap<String, Session> sessions = hazelcastInstance.getMap(sessionMapName);
        MapSessionRepository repository = new MapSessionRepository(sessions);
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(sessionTimeoutMinutes));
        return repository;
    }
}
