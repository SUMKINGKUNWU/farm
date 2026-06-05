package com.farm.exchange.me;

import com.farm.exchange.auth.AuthPrincipal;
import com.farm.exchange.auth.AuthTokenService;
import com.farm.exchange.bulk.BulkTokenResponse;
import com.farm.exchange.bulk.BulkTokenService;
import com.farm.exchange.farm.ExpansionResponse;
import com.farm.exchange.farm.FarmService;
import com.farm.exchange.farm.PlayerSummaryResponse;
import com.farm.exchange.farm.SlotListResponse;
import com.farm.exchange.farm.SlotType;
import com.farm.exchange.market.MarketQuoteResponse;
import com.farm.exchange.market.MarketService;
import com.farm.exchange.market.MarketTradeRequest;
import com.farm.exchange.market.MarketTradeResponse;
import com.farm.exchange.private_trade.AcceptPrivateTradeRequest;
import com.farm.exchange.private_trade.CreatePrivateTradeRequest;
import com.farm.exchange.private_trade.PrivateTradeOfferResponse;
import com.farm.exchange.private_trade.PrivateTradeResponse;
import com.farm.exchange.private_trade.PrivateTradeService;
import com.farm.exchange.private_trade.PrivateTradeSettlementResponse;
import com.farm.exchange.production.HarvestResponse;
import com.farm.exchange.production.GrowthInstanceResponse;
import com.farm.exchange.production.InventoryResponse;
import com.farm.exchange.production.ProductionResponse;
import com.farm.exchange.production.ProductionService;
import com.farm.exchange.shop.PurchaseRequest;
import com.farm.exchange.shop.PurchaseResponse;
import com.farm.exchange.shop.ShopService;
import com.farm.exchange.user.SetTradePasswordRequest;
import com.farm.exchange.user.TradePasswordResponse;
import com.farm.exchange.user.UserService;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AuthTokenService authTokenService;
    private final UserService userService;
    private final FarmService farmService;
    private final ProductionService productionService;
    private final ShopService shopService;
    private final MarketService marketService;
    private final PrivateTradeService privateTradeService;
    private final BulkTokenService bulkTokenService;

    public MeController(
            AuthTokenService authTokenService,
            UserService userService,
            FarmService farmService,
            ProductionService productionService,
            ShopService shopService,
            MarketService marketService,
            PrivateTradeService privateTradeService,
            BulkTokenService bulkTokenService
    ) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.farmService = farmService;
        this.productionService = productionService;
        this.shopService = shopService;
        this.marketService = marketService;
        this.privateTradeService = privateTradeService;
        this.bulkTokenService = bulkTokenService;
    }

    @PostMapping("/trade-password")
    public TradePasswordResponse setTradePassword(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody SetTradePasswordRequest request) {
        return userService.setTradePassword(currentUserId(authorization), request);
    }

    @GetMapping("/summary")
    public PlayerSummaryResponse summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return farmService.summary(currentUserId(authorization));
    }

    @GetMapping("/farm/plots")
    public SlotListResponse farmPlots(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return farmService.farmPlots(currentUserId(authorization));
    }

    @GetMapping("/ranch/slots")
    public SlotListResponse ranchSlots(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return farmService.ranchSlots(currentUserId(authorization));
    }

    @PostMapping("/farm/expand")
    public ExpansionResponse expandFarm(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return farmService.expand(currentUserId(authorization), SlotType.FARM);
    }

    @PostMapping("/ranch/expand")
    public ExpansionResponse expandRanch(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return farmService.expand(currentUserId(authorization), SlotType.RANCH);
    }

    @GetMapping("/inventory")
    public List<InventoryResponse> inventory(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return productionService.inventory(currentUserId(authorization));
    }

    @GetMapping("/growth")
    public List<GrowthInstanceResponse> growthInstances(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return productionService.growthInstances(currentUserId(authorization));
    }

    @PostMapping("/farm/plots/{plotId}/plant")
    public ProductionResponse plant(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID plotId, @RequestParam String itemCode) {
        return productionService.start(currentUserId(authorization), "FARM", plotId, itemCode);
    }

    @PostMapping("/ranch/slots/{slotId}/raise")
    public ProductionResponse raise(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID slotId, @RequestParam String itemCode) {
        return productionService.start(currentUserId(authorization), "RANCH", slotId, itemCode);
    }

    @PostMapping("/growth/{growthId}/harvest")
    public HarvestResponse harvest(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID growthId) {
        return productionService.harvest(currentUserId(authorization), growthId);
    }

    @PostMapping("/shop/purchase")
    public PurchaseResponse purchase(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody PurchaseRequest request) {
        return shopService.purchase(currentUserId(authorization), request);
    }

    @PostMapping("/market/buy")
    public MarketTradeResponse buy(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody MarketTradeRequest request) {
        return marketService.trade(currentUserId(authorization), "BUY", request);
    }

    @PostMapping("/market/sell")
    public MarketTradeResponse sell(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody MarketTradeRequest request) {
        return marketService.trade(currentUserId(authorization), "SELL", request);
    }

    @GetMapping("/market/items/{itemCode}/quote")
    public MarketQuoteResponse quote(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable String itemCode) {
        currentUserId(authorization);
        return marketService.quote(itemCode);
    }

    @GetMapping("/private-trades")
    public List<PrivateTradeOfferResponse> privateTrades(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return privateTradeService.offers(currentUserId(authorization));
    }

    @PostMapping("/private-trades")
    public PrivateTradeResponse createPrivateTrade(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody CreatePrivateTradeRequest request) {
        return privateTradeService.create(currentUserId(authorization), request);
    }

    @PostMapping("/private-trades/{offerId}/accept")
    public PrivateTradeSettlementResponse acceptPrivateTrade(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID offerId, @Valid @RequestBody AcceptPrivateTradeRequest request) {
        return privateTradeService.accept(currentUserId(authorization), offerId, request);
    }

    @PostMapping("/private-trades/{offerId}/cancel")
    public PrivateTradeResponse cancelPrivateTrade(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID offerId) {
        return privateTradeService.cancel(currentUserId(authorization), offerId);
    }

    @GetMapping("/bulk-tokens")
    public List<BulkTokenResponse> bulkTokens(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return bulkTokenService.list(currentUserId(authorization));
    }

    private UUID currentUserId(String authorization) {
        AuthPrincipal principal = authTokenService.require(authorization);
        return principal.getUserId();
    }
}
