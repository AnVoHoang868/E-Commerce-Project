package com.dev.dungcony.modules.product.services.interfaces.item;

import com.dev.dungcony.modules.product.dtos.req.ItemUpdateReq;

public interface ItemUpdateService {
    void updateQuantity(ItemUpdateReq item);

    void reduce(int productId, int sizeId, int quantity);

    void increase(int productId, int sizeId, int quantity);
}
