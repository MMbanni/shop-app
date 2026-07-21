package com.mbanni.shop.cart;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
public class Cart {

    public static final int MAX_QUANTITY = 999;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany( mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<CartItem> items = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "user_id",nullable = false, unique = true )
    private User user;

    // JPA requires no-arg constructor
    public Cart() {}

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user=user;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }


    public void addItem(Product product, int quantity) {
        if(product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if(quantity>MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.EXCEEDED_QUANTITY_LIMIT);
        }

        if (quantity<1) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        CartItem existingItem = findItemByProductId(product.getId());

        if(existingItem != null) {
            if(existingItem.getQuantity() + quantity>MAX_QUANTITY) {
                throw new BusinessException(ErrorCode.EXCEEDED_QUANTITY_LIMIT);
            }
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return;
        }

        CartItem item = new CartItem(this, product, quantity);
        items.add(item);
    }

    public void removeItem(Long cartItemId, int quantity) {

        if (quantity<1)  throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);

        CartItem foundItem = findItemById(cartItemId);

        if(foundItem == null) throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);

        int remainingQuantity = foundItem.getQuantity() - quantity;

        if (foundItem.getQuantity() < quantity) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        if(remainingQuantity == 0) {
            items.remove(foundItem);
            foundItem.detachFromCart(); // Object stays clean
        } else {
            foundItem.setQuantity(remainingQuantity);
        }
    }

    public void removeAll(Long cartItemId) {

        CartItem foundItem = findItemById(cartItemId);

        if(foundItem == null) throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);

        items.remove(foundItem);

    }

    public BigDecimal calculateTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for(CartItem item: items) {
            total = total.add(item.calculateLineTotal());
        }

        return total;
    }


    protected CartItem findItemById(Long id) {
        for (CartItem item : items) {
            if (Objects.equals(item.getId(), id)) {
                return item;
            }
        }

        return null;
    }

    protected CartItem findItemByProductId(Long productId) {
        for (CartItem item : items) {
            if (Objects.equals(item.getProduct().getId(), productId)) {
                return item;
            }
        }

        return null;
    }
}
