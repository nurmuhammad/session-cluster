package com.smartbox.sessioncluster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security учун фойдаланувчини Postgres'дан юклайди.
 *
 * <p>Логин пайтида {@link #loadUserByUsername(String)} {@code app_users}'дан
 * ёзувни олиб, уни Spring Security'нинг {@link UserDetails}'ига айлантиради.
 * Паролни (BCrypt) солиштириш кейин Security филтрида бажарилади — бу класс
 * фақат фойдаланувчини топиб беради.
 *
 * <p>{@code repository} constructor-injection орқали келади; конструкторни
 * Lombok'нинг {@link RequiredArgsConstructor}'и {@code final} майдондан
 * генерация қилади (Spring ягона конструкторни автомат inject қилади).
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Топилмади: " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}
