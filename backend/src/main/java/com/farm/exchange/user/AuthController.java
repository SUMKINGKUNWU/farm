package com.farm.exchange.user;

import com.farm.exchange.auth.AuthPrincipal;
import com.farm.exchange.auth.AuthTokenService;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final AuthTokenService authTokenService;

    public AuthController(UserService userService, AuthTokenService authTokenService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/auth/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/auth/me")
    public CurrentUserResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthPrincipal principal = authTokenService.require(authorization);
        return userService.currentUser(principal.getUserId());
    }

    @PostMapping("/users/{userId}/trade-password")
    public TradePasswordResponse setTradePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody SetTradePasswordRequest request
    ) {
        return userService.setTradePassword(userId, request);
    }
}
