package com.example.product_service.repository;

import com.example.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Atomic decrement executed by the database, so concurrent sales of the same product
     * (consumed from different partitions) cannot overwrite each other. Never goes below zero.
     *
     * @return number of rows updated (0 when the product does not exist)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Product p
            set p.stock = case when p.stock >= :quantity then p.stock - :quantity else 0 end
            where p.id = :id""")
    int decrementStock(Long id, int quantity);
}
