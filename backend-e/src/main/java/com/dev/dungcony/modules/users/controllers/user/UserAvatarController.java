package com.dev.dungcony.modules.users.controllers.user;

import com.dev.dungcony.commons.dtos.AccountDetails;
import com.dev.dungcony.commons.dtos.ApiRes;
import com.dev.dungcony.modules.users.dtos.res.UserRes;
import com.dev.dungcony.modules.users.entities.User;
import com.dev.dungcony.modules.users.exceptions.UserNotFound;
import com.dev.dungcony.modules.users.mappers.UserMapper;
import com.dev.dungcony.modules.users.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Users")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/user/update")
public class UserAvatarController {

    private static final long MAX_AVATAR_SIZE = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final UserRepository userRepository;

    @Operation(summary = "Upload my avatar")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiRes<UserRes>> uploadAvatar(
            @AuthenticationPrincipal AccountDetails details,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Ảnh đại diện không được vượt quá 2MB");
        }

        User user = userRepository.findById(details.requireUserUuid())
                .orElseThrow(UserNotFound::new);

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };

        Path uploadDir = Path.of("uploads", "avatars").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String fileName = user.getId() + "-" + UUID.randomUUID() + extension;
        Path destination = uploadDir.resolve(fileName);
        file.transferTo(destination);

        String avatarUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/uploads/avatars/" + fileName)
                .replaceQuery(null)
                .build()
                .toUriString();

        user.setAvatar(avatarUrl);
        userRepository.save(user);

        return ResponseEntity.ok(ApiRes.success("avatar uploaded", UserMapper.toUserDto(user)));
    }
}
