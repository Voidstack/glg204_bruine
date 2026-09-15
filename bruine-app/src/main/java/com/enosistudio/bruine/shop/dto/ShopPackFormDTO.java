package com.enosistudio.bruine.shop.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record ShopPackFormDTO(
        String name,
        String emoji,
        Integer points,
        Integer bonusPoints,
        Double priceEuros,
        Integer sortOrder,
        Boolean popular,
        Integer promoPercent,
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime promoStart,
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime promoEnd) {
}
