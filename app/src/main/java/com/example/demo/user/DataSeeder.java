package com.example.demo.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ilova ishga tushganda Postgres'da test foydalanuvchi yaratadi (agar yo'q bo'lsa).
 * Login: admin / parol: admin123
 *
 * MUHIM: app1 va app2 bir vaqtda ishga tushadi, shuning uchun "topilmasa -> saqla"
 * tekshiruvi ikkala instance o'rtasida atomar EMAS: ikkalasi ham bo'sh deb ko'rib
 * saqlashi mumkin. Shu sababli unique-constraint qarama-qarshiligini xotirjam
 * yutamiz — bu boshqa instance allaqachon seed qilganini bildiradi.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.findByUsername("admin").isPresent()) {
            return;
        }
        try {
            repository.save(new AppUser(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ADMIN"));
        } catch (DataIntegrityViolationException alreadySeeded) {
            // Boshqa instance ayni paytda admin'ni yaratib ulgurdi — muammo emas.
        }
    }
}
