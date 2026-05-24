package com.dev.dungcony.modules.users.services.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dev.dungcony.modules.users.dtos.res.UserRes;
import com.dev.dungcony.modules.users.dtos.req.UserUpdateReq;
import com.dev.dungcony.modules.users.entities.User;
import com.dev.dungcony.modules.users.exceptions.UserNotFound;
import com.dev.dungcony.modules.users.exceptions.UserUnAuthor;
import com.dev.dungcony.modules.users.mappers.UserMapper;
import com.dev.dungcony.modules.users.repositories.UserRepository;
import com.dev.dungcony.modules.users.services.interfaces.UserUpdateService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserUpdateImpl implements UserUpdateService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserRes updateUser(Integer accId, UserUpdateReq req) {

        UUID uuid = req.id();
        User user = userRepository.findById(uuid)
                .orElseThrow(UserNotFound::new);

        if (user.getAccountId() == null || !user.getAccountId().equals(accId))
            throw new UserUnAuthor();

        if (req.firstName() != null)
            user.setFirstName(req.firstName());
        if (req.lastName() != null)
            user.setLastName(req.lastName());
        if (req.avatar() != null)
            user.setAvatar(req.avatar());

        userRepository.save(user);

        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserRes adminUpdateUser(UserUpdateReq req) {
        UUID uuid = req.id();
        User user = userRepository.findById(uuid)
                .orElseThrow(UserNotFound::new);

        if (req.firstName() != null)
            user.setFirstName(req.firstName());
        if (req.lastName() != null)
            user.setLastName(req.lastName());
        if (req.avatar() != null)
            user.setAvatar(req.avatar());

        userRepository.save(user);

        return UserMapper.toUserDto(user);
    }

}
