package com.dev.dungcony.modules.auth.services.impl;

import com.dev.dungcony.modules.auth.config.JwtConfig;
import com.dev.dungcony.modules.auth.dtos.AccDto;
import com.dev.dungcony.modules.auth.dtos.req.RegisReq;
import com.dev.dungcony.modules.auth.dtos.res.AcessTokenRes;
import com.dev.dungcony.modules.auth.dtos.res.LoginResult;
import com.dev.dungcony.modules.auth.exceptions.IncorrectInput;
import com.dev.dungcony.modules.auth.enums.Status;
import com.dev.dungcony.modules.auth.services.interfaces.*;
import com.dev.dungcony.modules.users.exceptions.UserNotFound;
import com.dev.dungcony.modules.users.services.interfaces.UserCreateService;
import com.dev.dungcony.modules.users.services.interfaces.UserGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthImpl implements AuthService {

    private final AccountGetService accGetService;
    private final AccountCheckService accountCheckService;
    private final AccountCreateService accountCreateService;
    private final SendOtpService sendOtpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;
    private final UserGetService userGetService;
    private final UserCreateService userCreateService;

    @Transactional
    @Override
    public void register(RegisReq req) {

        log.info("Bắt đầu đăng ký tài khoản email={}, tên đăng nhập={}", req.email(), req.username());

        accountCheckService.existsByEmail(req.email());
        accountCheckService.existsByUsername(req.username());

        accountCreateService.createAccount(req.email(), req.username(), passwordEncoder.encode(req.password()));
        sendOtpService.sendOtpRegister(req.email());
    }

    @Override
    public LoginResult login(String username, String password, String deviceId) {

        log.info("Bắt đầu đăng nhập tài khoản tên đăng nhập={}", username);

        AccDto acc = accGetService.getByUsername(username);

        if (!passwordEncoder.matches(password, acc.password()))
            throw new IncorrectInput();

        if (!acc.verify())
            throw new IncorrectInput();

        if (acc.status() != Status.ACTIVE)
            throw new IncorrectInput(); // account bị khóa

        log.info("Đăng nhập thành công tài khoản: {}", acc.username());

        String acessToken = jwtService.generateToken(acc.id(), acc.username(), acc.role(), resolveUserUuid(acc.id()));
        String refreshToken = refreshTokenService.create(acc.id(), deviceId);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(jwtConfig.isRefreshCookieSecure())
                .path(jwtConfig.getRefreshCookiePath())
                .sameSite("Lax")
                .maxAge(jwtConfig.getRefreshExpiration())
                .build();
        log.info("Đã tạo refresh token cho tài khoản: {}", acc.username());

        return new LoginResult(acessToken, JwtConfig.headerPrefix, jwtConfig.getExpiration(), refreshCookie.toString());
    }

    @Override
    public AcessTokenRes refreshToken(String refreshToken) {
        AccDto acc = refreshTokenService.verify(refreshToken);
        String token = jwtService.generateToken(acc.id(), acc.username(), acc.role(), resolveUserUuid(acc.id()));

        return new AcessTokenRes(token, jwtConfig.getExpiration());
    }

    // -----PRIVATE-----//
    private UUID resolveUserUuid(int accId) {
        try {
            return userGetService.getUserByAccId(accId).id();
        } catch (UserNotFound e) {
            log.info("Không tìm thấy hồ sơ người dùng cho accId {}, bắt đầu khởi tạo", accId);
            try {
                return userCreateService.createUser(accId).id();
            } catch (Exception ex) {
                log.warn("Tạo hồ sơ người dùng thất bại (có thể do xử lý đồng thời), thử lấy lại hồ sơ: {}", ex.getMessage());
                return userGetService.getUserByAccId(accId).id();
            }
        }
    }

    @Override
    public void logout(String refreshToken, String deviceId) {
        refreshTokenService.revoke(refreshToken, deviceId);
    }
}
