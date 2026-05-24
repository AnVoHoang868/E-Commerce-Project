package com.dev.dungcony.modules.product.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dev.dungcony.modules.product.entities.Item;
import com.dev.dungcony.modules.product.entities.ItemId;

public interface ItemRepository extends JpaRepository<Item, ItemId> {
    @Query("""
            select i from Item i
            join fetch i.product
            join fetch i.size
            where i.id.productId = :productId
            """)
    List<Item> findByIdProductId(Integer productId);

    @Query("""
            select i from Item i
            join fetch i.product
            join fetch i.size
            where i.id.sizeId = :sizeId
            """)
    List<Item> findByIdSizeId(Integer sizeId);

    Optional<Item> findItemById(ItemId id);

    @Query("""
            select i
            from Item i
            join fetch i.product
            join fetch i.size
            where i.id = :id
            """)
    Optional<Item> findDetailByIdWithProductAndSize(ItemId id);

    @Query("""
            select i.id.sizeId
            from Item i
            where i.product.code = :productCode
            """)
    List<Integer> findSizesByProductCode(String productCode);

    @Query("""
            select i from Item i
            join fetch i.product
            join fetch i.size
            where i.product.code in :productCodes
            """)
    List<Item> findByProductCodes(List<String> productCodes);
}
