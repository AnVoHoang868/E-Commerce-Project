package com.dev.dungcony.modules.users.services.interfaces;

import java.util.List;
import java.util.UUID;

import com.dev.dungcony.modules.users.dtos.res.UserRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGetService {
    UserRes getUserByAccId(int accId);

    UserRes getUserById(UUID id);

    Page<UserRes> getAll(Pageable pageable);

    List<UserRes> getByName(String name);
}
