package com.farm.exchange.private_trade;

import java.util.UUID;
import javax.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/private-trades")
public class PrivateTradeController {

    private final PrivateTradeService privateTradeService;

    public PrivateTradeController(PrivateTradeService privateTradeService) {
        this.privateTradeService = privateTradeService;
    }

    @GetMapping
    public List<PrivateTradeOfferResponse> offers(@PathVariable UUID userId) {
        return privateTradeService.offers(userId);
    }

    @PostMapping
    public PrivateTradeResponse create(@PathVariable UUID userId, @Valid @RequestBody CreatePrivateTradeRequest request) {
        return privateTradeService.create(userId, request);
    }

    @PostMapping("/{offerId}/accept")
    public PrivateTradeSettlementResponse accept(@PathVariable UUID userId, @PathVariable UUID offerId, @Valid @RequestBody AcceptPrivateTradeRequest request) {
        return privateTradeService.accept(userId, offerId, request);
    }

    @PostMapping("/{offerId}/cancel")
    public PrivateTradeResponse cancel(@PathVariable UUID userId, @PathVariable UUID offerId) {
        return privateTradeService.cancel(userId, offerId);
    }
}
