package com.mbanni.shop.support;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.order.*;
import com.mbanni.shop.order.dto.OrderSnapshot;
import com.mbanni.shop.payment.dto.StripeSessionSnapshot;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.user.User;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class TestSetup {
    private TestSetup() {}
    public static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final String URL = "https://checkout.example.test/";

    public static User user() {
        User u = new User();
        id(u, 7L);
        u.setName("User");
        u.setEmail("User@example.test");
        u.setPassword("unused-test-hash");
        u.activate();
        u.assignCart(new Cart());
        return u;
    }
    public static Product product() {
        Product p = new Product("Chair", new BigDecimal("100.00"), "Test only", "test");
        id(p, 20L);
        p.setStock(10);
        version(p,3);
        p.activate();
        return p;
    }
    public static Order order(User u, Product p) {
        Order o = new Order(u, NOW.minusSeconds(60), NOW.plusSeconds(2100));
        id(o, 50L);

        o.configureCheckout(
                "https://shop.example.test/success",
                "https://shop.example.test/cancel"
        );

        OrderItem line = new OrderItem(10L, p.getId(), p.getName(), 2, p.getPrice());
        id(line, 100L);
        o.addItem(line);
        return o;
    }

    public static OrderSnapshot snapshot(String sessionId, Instant createdAt) {
        return new OrderSnapshot(
                50L,
                7L,
                OrderStatus.PENDING,
                sessionId,
                sessionId == null ? null : URL,
                createdAt,
                createdAt.plusSeconds(2160),
                null,
                new BigDecimal("200.00"),
                "https://shop.example.test/success",
                "https://shop.example.test/cancel",
                null,
                List.of(new OrderSnapshot.Line(
                        100L,
                        "Chair",
                        2,
                        new BigDecimal("100.00")
                ))
        );
    }

    public static OrderSnapshot status(OrderSnapshot o, OrderStatus status) {
        return new OrderSnapshot(
                o.orderId(), o.userId(), status, o.stripeSessionId(), o.checkoutUrl(),
            o.createdAt(), o.expiresAt(), status == OrderStatus.PAID ? NOW : null, o.total(),
            o.successUrl(), o.cancelUrl(), o.reviewNeededAt(), o.items());
    }

    public static StripeSessionSnapshot session(String status, String payment) {
        return new StripeSessionSnapshot(
                "cs_test",
                "open".equals(status) ? URL : null,
                status,
                payment,
                "payment",
                "sek",
                20000L,
                "50",
                Map.of("orderId", "50", "userId", "7")
        );
    }
    public static void id(Object o, long id) {
        ReflectionTestUtils.setField(o, "id", id);
    }
    public static void version(Object o, long version) {
        ReflectionTestUtils.setField(o, "version", version);
    }

}
