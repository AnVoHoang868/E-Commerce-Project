package com.dev.dungcony.modules.users.mappers;

import com.dev.dungcony.modules.users.dtos.res.UserRes;
import com.dev.dungcony.modules.users.entities.User;

public class UserMapper {

    public static UserRes toUserDto(User user) {
        return new UserRes(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatar());
    }
}
