package com.dev.dungcony.modules.cart.services.impl;

import com.dev.dungcony.modules.cart.dtos.CartItemDto;
import com.dev.dungcony.modules.cart.dtos.req.AddToCartReq;
import com.dev.dungcony.modules.cart.dtos.req.UpdateCartItemReq;
import com.dev.dungcony.modules.cart.entities.CartItem;
import com.dev.dungcony.modules.cart.entities.CartItemId;
import com.dev.dungcony.modules.cart.exceptions.CartItemNotFoundException;
import com.dev.dungcony.modules.cart.exceptions.CartUnProcessableException;
import com.dev.dungcony.modules.cart.exceptions.NotAddOutOfStockProduct;
import com.dev.dungcony.modules.cart.repositories.CartRepository;
import com.dev.dungcony.modules.cart.services.interfaces.CartUpdateService;
import com.dev.dungcony.modules.product.dtos.ProductDto;
import com.dev.dungcony.modules.product.entities.Product;
import com.dev.dungcony.modules.product.entities.Size;
import com.dev.dungcony.modules.product.enums.ProductSize;
import com.dev.dungcony.modules.product.enums.ProductStatus;
import com.dev.dungcony.modules.product.services.interfaces.SizeCacheService;
import com.dev.dungcony.modules.product.services.interfaces.item.ItemUpdateService;
import com.dev.dungcony.modules.product.services.interfaces.product.ProductGetService;
import com.dev.dungcony.modules.users.entities.User;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartUpdateImpl implements CartUpdateService {

    private final CartRepository cartItemRepository;
    private final ItemUpdateService itemUpdateService;
    private final SizeCacheService sizeCacheService;
    private final ProductGetService productGetService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void addItemToCart(UUID userId, AddToCartReq req) {
        ProductDto productDto = productGetService.getDtoByCode(req.productCode());
        Integer sizeId = sizeCacheService.getIdBySize(req.size());

        if (productDto.status() == ProductStatus.OUT_OF_STOCK) {
            throw new NotAddOutOfStockProduct();
        }
        CartItem cartItem = cartItemRepository
                .findById_UserIdAndId_ProductIdAndId_SizeId(userId, productDto.id(), sizeId)
                .orElse(null);

        // giảm số lượng sản phẩm
        itemUpdateService.reduce(productDto.id(), sizeId, req.quantity());

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setId(new CartItemId(userId, productDto.id(), sizeId));
            cartItem.setUser(entityManager.getReference(User.class, userId));
            cartItem.setProduct(entityManager.getReference(Product.class, productDto.id()));
            cartItem.setSize(entityManager.getReference(Size.class, sizeId));
            cartItem.setQuantity(req.quantity());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + req.quantity());
        }

        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void removeItemFromCart(UUID userId, String productCode, ProductSize size) {
        int productId = productGetService.getIdByCode(productCode);
        int sizeId = sizeCacheService.getIdBySize(size);

        CartItem item = cartItemRepository.findById(new CartItemId(userId, productId, sizeId))
                .orElseThrow(CartItemNotFoundException::new);

        itemUpdateService.increase(productId, sizeId, item.getQuantity());
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void updateItemQuantity(UUID userId, UpdateCartItemReq req) {
        int productId = productGetService.getIdByCode(req.productCode());
        int sizeId = sizeCacheService.getIdBySize(req.size());

        CartItem cartItem = cartItemRepository.findById(new CartItemId(userId, productId, sizeId))
                .orElseThrow(CartItemNotFoundException::new);

        int delta = req.quantity() - cartItem.getQuantity();
        if (delta > 0) {
            itemUpdateService.reduce(productId, sizeId, delta);
        } else if (delta < 0) {
            itemUpdateService.increase(productId, sizeId, Math.abs(delta));
        }

        cartItem.setQuantity(req.quantity());
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void removeListItem(UUID uid, List<CartItemDto> cartItemDtos) {
        removeListItem(uid, cartItemDtos, false);
    }

    @Override
    @Transactional
    public void removeListItemAndRestoreStock(UUID uid, List<CartItemDto> cartItemDtos) {
        removeListItem(uid, cartItemDtos, true);
    }

    private void removeListItem(UUID uid, List<CartItemDto> cartItemDtos, boolean restoreStock) {
        List<CartItemId> ids = cartItemDtos.stream()
                .map(item -> toCartItemId(uid, item, restoreStock))
                .distinct()
                .toList();

        List<CartItem> items = cartItemRepository.findAllById(ids);
        if (items.size() != ids.size()) {
            throw new CartUnProcessableException();
        }

        if (restoreStock) {
            for (CartItem item : items) {
                itemUpdateService.increase(
                        item.getId().getProductId(),
                        item.getId().getSizeId(),
                        item.getQuantity());
            }
        }

        cartItemRepository.deleteAll(items);
    }

    private CartItemId toCartItemId(UUID uid, CartItemDto item, boolean resolveByProductCode) {
        if (item == null || item.productSize() == null) {
            throw new CartUnProcessableException();
        }

        Integer productId;
        if (resolveByProductCode || item.productId() == null) {
            if (item.productCode() == null || item.productCode().isBlank()) {
                throw new CartUnProcessableException();
            }
            productId = productGetService.getIdByCode(item.productCode());
        } else {
            productId = item.productId();
        }

        return new CartItemId(uid, productId, sizeCacheService.getIdBySize(item.productSize()));
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        List<CartItem> items = cartItemRepository.findAllByUserId(userId);
        for (CartItem item : items) {
            itemUpdateService.increase(
                    item.getId().getProductId(),
                    item.getId().getSizeId(),
                    item.getQuantity());
        }
        cartItemRepository.deleteAllById_UserId(userId);
    }
}
