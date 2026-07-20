package com.example.demo.user;

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
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public AppUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

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
