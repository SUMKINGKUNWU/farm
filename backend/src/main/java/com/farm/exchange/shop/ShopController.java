package com.farm.exchange.shop;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping("/purchase")
    public PurchaseResponse purchase(@PathVariable UUID userId, @Valid @RequestBody PurchaseRequest request) {
        return shopService.purchase(userId, request);
    }
}

