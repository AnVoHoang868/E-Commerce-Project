package com.dev.dungcony.modules.users.services.interfaces;

import com.dev.dungcony.modules.users.dtos.ReceiverDto;
import com.dev.dungcony.modules.users.dtos.res.ReceiverRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RecieverGetService {
    ReceiverDto getById(UUID userId, Integer id);

    ReceiverRes getReceiverById(UUID userId, Integer id);

    ReceiverRes adminGetReceiverById(Integer id);

    Page<ReceiverRes> adminGetAll(Pageable pageable);

    List<ReceiverRes> getAllByUser(UUID userId);
}
