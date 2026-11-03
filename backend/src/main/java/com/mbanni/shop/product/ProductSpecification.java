package com.mbanni.shop.product;

import com.mbanni.shop.product.command.FilterProductCommand;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductSpecification {

    public static Specification<Product> filter(
            FilterProductCommand request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Price filtering
            if (request.priceQuery() != null) {
                var price = request.priceQuery();

                if (price.min() != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(
                                    root.get("price"), price.min()
                            )
                    );
                }

                if (price.max() != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(
                                    root.get("price"), price.max()
                            )
                    );
                }
            }

            // Attribute filtering
            if (request.filters() != null) {

                for (Map.Entry<String, List<String>> entry
                        : request.filters().entrySet()) {

                    String key = entry.getKey();
                    List<String> values = entry.getValue();

                    if (values == null || values.isEmpty()) {
                        continue;
                    }

                    Expression<String> attribute = cb.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("attributes"),
                            cb.literal(key)
                    );

                    // OR within the same category
                    List<Predicate> orPredicates = new ArrayList<>();

                    for (String value : values) {
                        orPredicates.add(
                                cb.equal(attribute, value)
                        );
                    }

                    predicates.add(
                            cb.or(orPredicates.toArray(new Predicate[0]))
                    );
                }
            }

            // AND between categories and price conditions
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}