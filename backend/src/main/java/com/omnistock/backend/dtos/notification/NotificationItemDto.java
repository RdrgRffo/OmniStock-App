package com.omnistock.backend.dtos.notification;

import com.omnistock.backend.entity.AppNotificationType;

import java.time.LocalDateTime;

public record NotificationItemDto(
        Long id,
        AppNotificationType type,
        String title,
        String message,
        Integer supplierId,
        String supplierName,
        Integer productId,
        String productLabel,
        String actionPath,
        String scopeTag,
        LocalDateTime createdAt,
        boolean read
) {
}
