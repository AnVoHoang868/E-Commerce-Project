package com.dev.dungcony.modules.notifications.services.interfaces;

import com.dev.dungcony.modules.notifications.dtos.res.NotificationRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationGetService {
    Page<NotificationRes> getAllBySender(Pageable pageable, UUID senderId);

    Page<NotificationRes> getAllForAdmin(Pageable pageable);

    long countUnRead(UUID senderId);

    long countUnRead();
}
