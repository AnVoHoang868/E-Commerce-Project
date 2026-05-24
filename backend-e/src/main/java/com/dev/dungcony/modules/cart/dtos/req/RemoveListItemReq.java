package com.dev.dungcony.modules.cart.dtos.req;

import com.dev.dungcony.modules.cart.dtos.CartItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RemoveListItemReq(
        @Valid
        @NotEmpty(message = "listRemove không được trống")
        List<CartItemDto> listRemove
) {
}
