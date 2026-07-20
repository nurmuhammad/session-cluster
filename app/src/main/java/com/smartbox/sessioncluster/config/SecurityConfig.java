package com.smartbox.sessioncluster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form-login, BCrypt парол ва CSRF'ни созлайди.
 *
 * <p>CSRF token сессияда сақланади, сессия эса Hazelcast орқали умумий — шунинг
 * учун token иккала нусхада бир хил кўринади. Session clustering тўғри ишласа,
 * бир нусха берган форма token'ини бошқа нусха ҳам тан олади.
 */
@Configuration
public class SecurityConfig {

    /**
     * Хавфсизлик занжири: {@code /login}, {@code /error} ва статик ресурслар очиқ,
     * қолган ҳамма нарса аутентификация талаб қилади.
     *
     * <p><b>{@code /error} албатта {@code permitAll} бўлиши шарт.</b> Акс ҳолда
     * браузер параллел сўрайдиган {@code /favicon.ico} (404 → {@code /error})
     * ҳимояланган бўлиб қолади: Spring saved-request яратиб redirect қилади ва
     * <b>янги</b> сессия очади. Шунда логин формасидаги CSRF token битта сессияда,
     * браузер cookie'си бошқасида қолиб, {@code POST /login} «Invalid CSRF» (403)
     * билан логинга қайтаверади. Бу энг қийин топилган баг эди.
     *
     * @return созланган {@link SecurityFilterChain}
     * @throws Exception {@link HttpSecurity} қуришда хатолик юз берса
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // /login, /error ва статик ресурслар очиқ (/error нима учун очиқлигини — JavaDoc'га қаранг)
                .requestMatchers("/login", "/error", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Қолгани фақат логин қилганлар учун
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        // CSRF ёқилган (default). Token сессияда → Hazelcast орқали умумий.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
