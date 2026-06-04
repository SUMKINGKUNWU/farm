package com.farm.exchange.farm;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping("/summary")
    public PlayerSummaryResponse summary(@PathVariable UUID userId) {
        return farmService.summary(userId);
    }

    @GetMapping("/farm/plots")
    public SlotListResponse farmPlots(@PathVariable UUID userId) {
        return farmService.farmPlots(userId);
    }

    @GetMapping("/ranch/slots")
    public SlotListResponse ranchSlots(@PathVariable UUID userId) {
        return farmService.ranchSlots(userId);
    }

    @PostMapping("/farm/expand")
    public ExpansionResponse expandFarm(@PathVariable UUID userId) {
        return farmService.expand(userId, SlotType.FARM);
    }

    @PostMapping("/ranch/expand")
    public ExpansionResponse expandRanch(@PathVariable UUID userId) {
        return farmService.expand(userId, SlotType.RANCH);
    }
}

