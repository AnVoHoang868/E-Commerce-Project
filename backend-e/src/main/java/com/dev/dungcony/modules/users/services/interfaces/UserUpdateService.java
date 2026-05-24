package com.dev.dungcony.modules.users.services.interfaces;

import com.dev.dungcony.modules.users.dtos.res.UserRes;
import com.dev.dungcony.modules.users.dtos.req.UserUpdateReq;

public interface UserUpdateService {
    UserRes updateUser(Integer accId, UserUpdateReq req);

    UserRes adminUpdateUser(UserUpdateReq req);
}
