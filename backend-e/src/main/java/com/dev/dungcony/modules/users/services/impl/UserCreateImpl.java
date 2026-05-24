package com.dev.dungcony.modules.users.services.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dev.dungcony.modules.users.dtos.res.UserRes;
import com.dev.dungcony.modules.users.entities.User;
import com.dev.dungcony.modules.users.mappers.UserMapper;
import com.dev.dungcony.modules.users.repositories.UserRepository;
import com.dev.dungcony.modules.users.services.interfaces.UserCreateService;
import com.dev.dungcony.modules.voucher.services.interfaces.UserVoucherCreateService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCreateImpl implements UserCreateService {
    private final UserRepository userRepository;
    private final UserVoucherCreateService userVoucherService;

    @Transactional
    @Override
    public UserRes createUser(int accId) {
        User user = new User();

        UUID uuid = UUID.randomUUID();
        user.setId(uuid);
        user.setAccountId(accId);
        log.info("Bắt đầu khởi tạo hồ sơ người dùng: {}", user.getId());
        userRepository.saveAndFlush(user);
        log.info("Đã tạo hồ sơ người dùng: {}", user.getId());
        userVoucherService.applyNewbieVoucher(uuid);

        return UserMapper.toUserDto(user);
    }

}
