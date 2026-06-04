package com.farm.exchange.market;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/buy")
    public MarketTradeResponse buy(@PathVariable UUID userId, @Valid @RequestBody MarketTradeRequest request) {
        return marketService.trade(userId, "BUY", request);
    }

    @PostMapping("/sell")
    public MarketTradeResponse sell(@PathVariable UUID userId, @Valid @RequestBody MarketTradeRequest request) {
        return marketService.trade(userId, "SELL", request);
    }
}

