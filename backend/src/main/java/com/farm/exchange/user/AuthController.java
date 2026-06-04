package com.farm.exchange.user;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/users/{userId}/trade-password")
    public TradePasswordResponse setTradePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody SetTradePasswordRequest request
    ) {
        return userService.setTradePassword(userId, request);
    }
}

