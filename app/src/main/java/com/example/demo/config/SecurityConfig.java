package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Login sahifasi, xatolik sahifasi va statik resurslar ochiq.
                // MUHIM: /error ochiq bo'lishi SHART. Aks holda brauzer parallel
                // yuboradigan /favicon.ico (404 -> /error) himoyalangan bo'lib qoladi,
                // Spring saved-request + redirect qilib YANGI sessiya yaratadi. Shunda
                // login forma token'i bitta sessiyada, brauzer cookie'si boshqasida
                // qoladi -> POST /login'da "Invalid CSRF" (403) -> login sahifasiga qaytish.
                .requestMatchers("/login", "/error", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Qolgani faqat login qilganlar uchun
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
        // CSRF yoqilgan (default). Token sessiyada saqlanadi -> Hazelcast orqali umumiy.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
