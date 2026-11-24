package com.mbanni.shop.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Integer position;

    public ProductImage() {}

    public ProductImage(String url, Integer position) {
        this.url = url;
        this.position = position;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public Integer getPosition() { return position; }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}