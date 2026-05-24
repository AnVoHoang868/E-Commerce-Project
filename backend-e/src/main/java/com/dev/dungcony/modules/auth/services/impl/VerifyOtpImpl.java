package com.dev.dungcony.modules.auth.services.impl;

import com.dev.dungcony.modules.auth.dtos.req.VerifyOtpReq;
import com.dev.dungcony.modules.auth.enums.OtpType;
import com.dev.dungcony.modules.auth.exceptions.OtpExpire;
import com.dev.dungcony.modules.auth.exceptions.OtpIsIncorrect;
import com.dev.dungcony.modules.auth.helpers.Generate;
import com.dev.dungcony.modules.auth.services.interfaces.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class VerifyOtpImpl implements VerifyOtpService {
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final AccountUpdateService accountUpdateService;
    private final Generate generate;

    @Override
    public void verifyOtpRegister(VerifyOtpReq req) {
        String value = redisService.getValue(generate.key(req.email(), OtpType.REGISTER.getValue()));
        log.info("Đã đọc OTP đăng ký từ Redis cho email: {}", req.email());

        if (value == null)
            throw new OtpExpire();
        if (!passwordEncoder.matches(req.otp(), value))
            throw new OtpIsIncorrect();

        log.info("Xác thực OTP đăng ký thành công cho email: {}", req.email());
        accountUpdateService.verify(req.email());
        redisService.delete(generate.key(req.email(), OtpType.REGISTER.getValue()));
    }

    @Override
    public void verifyOtpEmailChange(int accid, String username, String newEmail, String otp) {
        String value = redisService.getValue(generate.key(username, OtpType.CHANGE_EMAIL.getValue()));
        log.info("Đã đọc OTP đổi email từ Redis cho tên đăng nhập: {}", username);

        if (value == null)
            throw new OtpExpire();
        if (!passwordEncoder.matches(otp, value))
            throw new OtpIsIncorrect();

        log.info("Xác thực OTP đổi email thành công cho tên đăng nhập: {}", username);

        accountUpdateService.updateEmail(accid, newEmail);
        redisService.delete(generate.key(username, OtpType.CHANGE_EMAIL.getValue()));
    }
}
