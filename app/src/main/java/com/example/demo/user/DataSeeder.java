package com.example.demo.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Илова ишга тушганда Postgres'да демо фойдаланувчи ({@code admin} / {@code admin123})
 * яратади — агар ҳали мавжуд бўлмаса.
 *
 * <p><b>Race-safe:</b> app1 ва app2 бир вақтда кўтарилади, шунинг учун
 * «топилмаса → сақла» текшируви иккала нусха орасида атомар эмас: иккови ҳам
 * бўш деб кўриб {@code admin}'ни ёзишга уриниши мумкин. Иккинчисида unique
 * constraint бузилиб {@link DataIntegrityViolationException} отилади; уни хотиржам
 * ютамиз, чунки бу «бошқа нусха аллақачон seed қилди» дегани — хатолик эмас.
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
            // Бошқа нусха айни пайтда admin'ни яратиб улгурди — муаммо эмас.
        }
    }
}
