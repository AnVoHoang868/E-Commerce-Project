package com.dev.dungcony.modules.notifications.services.impl;

import com.dev.dungcony.modules.notifications.dtos.res.NotificationRes;
import com.dev.dungcony.modules.notifications.repositories.NotificationRepository;
import com.dev.dungcony.modules.notifications.services.interfaces.NotificationGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationGetImpl implements NotificationGetService {

    private final NotificationRepository notificationRepo;

    @Override
    public Page<NotificationRes> getAllBySender(Pageable pageable, UUID senderId) {
        return notificationRepo.findAllBySender(senderId, pageable);
    }

    @Override
    public Page<NotificationRes> getAllForAdmin(Pageable pageable) {
        return notificationRepo.findAllForAdmin(pageable);
    }

    @Override
    public long countUnRead(UUID senderId) {
        return notificationRepo.countAllBySenderIdAndReaded(senderId, false);
    }

    @Override
    public long countUnRead() {
        return notificationRepo.countAllByForAdminAndReaded(true, false);
    }
}
