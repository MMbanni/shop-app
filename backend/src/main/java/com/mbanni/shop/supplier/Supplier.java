package com.mbanni.shop.supplier;

import com.mbanni.shop.product.Product;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "supplier")
    private List<Product> productList;

    public Supplier(){}
}
