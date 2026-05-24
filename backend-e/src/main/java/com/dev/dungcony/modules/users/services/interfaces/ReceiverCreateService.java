package com.dev.dungcony.modules.users.services.interfaces;

import com.dev.dungcony.modules.users.dtos.req.ReceiverCreateReq;
import com.dev.dungcony.modules.users.dtos.res.ReceiverRes;

import java.util.UUID;

public interface ReceiverCreateService {
    ReceiverRes create(UUID userId, ReceiverCreateReq req);
}
