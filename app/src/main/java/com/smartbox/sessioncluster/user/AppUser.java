package com.smartbox.sessioncluster.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Фойдаланувчи — Postgres'да сақланадиган ягона бизнес-дата.
 *
 * <p>Сессия бу ерда ЭМАС: у Hazelcast'да. Postgres фақат «ким рўйхатдан ўтган»
 * (логин, BCrypt hash, роль)ни билади, «ким ҳозир логин ҳолатда»ни эмас — бу
 * маълумот сессияда, яъни Hazelcast'да ётади.
 *
 * <p>Getter/setter'ларни Lombok генерация қилади. {@code @Data} атайлаб
 * ишлатилмади: JPA entity'да {@code equals/hashCode/toString} муаммоли
 * ({@code id} ўзгарувчан, {@code toString} lazy-load'ни триггерлаши мумкин) —
 * шунинг учун фақат {@code @Getter}/{@code @Setter}.
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor // JPA талаб қиладиган бўш конструктор (Lombok генерация қилади)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE) // id DB томонидан берилади — setter йўқ
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hash

    @Column(nullable = false)
    private String role = "USER";

    /** Янги фойдаланувчи; {@code id} DB томонидан берилади, шунинг учун конструкторда йўқ. */
    public AppUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
