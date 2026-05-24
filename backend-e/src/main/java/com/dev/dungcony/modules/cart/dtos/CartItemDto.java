package com.dev.dungcony.modules.cart.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dev.dungcony.modules.product.enums.ProductSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CartItemDto(
        @JsonIgnore
        Integer productId,
        @NotBlank
        String productCode,
        @JsonAlias("size")
        @NotNull
        ProductSize productSize,
        String categoryCode,
        String providerCode,
        BigDecimal originalPrice,
        BigDecimal finalPrice
) {
}
