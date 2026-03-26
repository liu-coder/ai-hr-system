package com.example.aihr.auth.web;

import com.example.aihr.auth.repo.UserRepository;
import com.example.aihr.auth.repo.UserRoleRepository;
import com.example.aihr.auth.security.JwtService;
import com.example.aihr.auth.domain.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, UserRoleRepository roles, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 登录并签发访问 Token。
     * <p>
     * 认证流程（当前实现）：
     * 1) 根据 `tenantId + username` 找到用户
     * 2) 校验密码（bcrypt）与用户是否 enabled
     * 3) 查询用户角色集合并签发 JWT
     *
     * @param req 请求体
     * @return `{ "tokenType": "Bearer", "accessToken": "..." }`
     */
    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse token(@Valid @RequestBody TokenRequest req) {
        UserEntity user = users.findByTenantIdAndUsername(req.tenantId(), req.username())
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("user disabled");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordBcrypt())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        List<String> roleList = roles.findRolesByUserId(user.getId());

        Map<String, Object> abac = new HashMap<>();
        abac.put("orgIds", List.of("org-demo"));
        abac.put("deptIds", List.of("dept-demo"));
        abac.put("securityLevel", "NORMAL");

        String accessToken = jwtService.issueAccessToken(user.getTenantId(), user.getId(), user.getUsername(), roleList, abac);
        return new TokenResponse("Bearer", accessToken);
    }

    /**
     * 登录请求体。
     */
    public record TokenRequest(
            /** 租户ID（必填） */
            @NotBlank String tenantId,
            /** 用户名（必填） */
            @NotBlank String username,
            /** 明文密码（必填；服务端会对比 bcrypt 摘要） */
            @NotBlank String password
    ) {}

    /**
     * 登录返回结果。
     */
    public record TokenResponse(String tokenType, String accessToken) {}
}

