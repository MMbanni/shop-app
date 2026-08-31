package com.mbanni.shop.user;

import com.mbanni.shop.cart.Cart;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private Instant suspendedUntil;

    public User() {}

    public Long getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public Role getRole() {
        return this.role;
    }

    public Cart getCart() { return this.cart; }

    public String getIp(){ return this.ip; }

    public UserStatus getStatus() { return this.status;}

    public Instant getSuspendedUntil() { return this.suspendedUntil;}

    public void setName(String name) {
        this.name=name;
    }

    public void setEmail(String email) {
        this.email=email;
    }

    public void setPassword(String password) {
        this.password=password;
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void assignCart(Cart cart) {
        this.cart = cart;

        if (cart != null && cart.getUser() != this) {
            cart.setUser(this);
        }
    }

    public void setStatus(UserStatus status) {
        this.status=status;
    }

    public void suspend(int days) {
        this.status=UserStatus.SUSPENDED;
        this.suspendedUntil = Instant.now().plus(days, ChronoUnit.DAYS);
    }
    public void ban () {
        this.status=UserStatus.SUSPENDED;
        this.suspendedUntil = null;

    }

    public void activate() {
        this.status=UserStatus.ACTIVE;
        this.suspendedUntil = null;
    }
}
