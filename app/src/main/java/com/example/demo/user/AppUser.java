package com.example.demo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Фойдаланувчи — Postgres'да сақланадиган ягона бизнес-дата.
 *
 * <p>Сессия бу ерда ЭМАС: у Hazelcast'да. Postgres фақат «ким рўйхатдан ўтган»
 * (логин, BCrypt hash, роль)ни билади, «ким ҳозир логин ҳолатда»ни эмас — бу
 * маълумот сессияда, яъни Hazelcast'да ётади.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hash

    @Column(nullable = false)
    private String role = "USER";

    public AppUser() {
    }

    public AppUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
