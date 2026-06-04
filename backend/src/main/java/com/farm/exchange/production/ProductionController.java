package com.farm.exchange.production;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/items")
    public List<ItemResponse> items(@RequestParam(required = false) String itemType) {
        return productionService.items(itemType);
    }

    @GetMapping("/users/{userId}/inventory")
    public List<InventoryResponse> inventory(@PathVariable UUID userId) {
        return productionService.inventory(userId);
    }

    @PostMapping("/users/{userId}/farm/plots/{plotId}/plant")
    public ProductionResponse plant(@PathVariable UUID userId, @PathVariable UUID plotId, @RequestParam String itemCode) {
        return productionService.start(userId, "FARM", plotId, itemCode);
    }

    @PostMapping("/users/{userId}/ranch/slots/{slotId}/raise")
    public ProductionResponse raise(@PathVariable UUID userId, @PathVariable UUID slotId, @RequestParam String itemCode) {
        return productionService.start(userId, "RANCH", slotId, itemCode);
    }

    @PostMapping("/users/{userId}/growth/{growthId}/harvest")
    public HarvestResponse harvest(@PathVariable UUID userId, @PathVariable UUID growthId) {
        return productionService.harvest(userId, growthId);
    }
}

