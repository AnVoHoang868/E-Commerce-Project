package com.dev.dungcony.modules.users.services.interfaces;

import com.dev.dungcony.modules.users.dtos.res.UserRes;

public interface UserCreateService {
    UserRes createUser(int accId);
}
