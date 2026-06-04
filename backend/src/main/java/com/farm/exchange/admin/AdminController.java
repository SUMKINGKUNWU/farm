package com.farm.exchange.admin;

import com.farm.exchange.auth.AuthPrincipal;
import com.farm.exchange.auth.AuthTokenService;
import com.farm.exchange.bulk.BulkTokenResponse;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthTokenService authTokenService;

    public AdminController(AdminService adminService, AuthTokenService authTokenService) {
        this.adminService = adminService;
        this.authTokenService = authTokenService;
    }

    @GetMapping("/tax-configs")
    public List<TaxConfigResponse> taxConfigs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthPrincipal admin = authTokenService.require(authorization);
        return adminService.taxConfigs(admin.getUserId());
    }

    @PutMapping("/tax-configs/{tradeType}")
    public TaxConfigResponse updateTaxConfig(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable String tradeType, @Valid @RequestBody TaxConfigRequest request) {
        AuthPrincipal admin = authTokenService.require(authorization);
        return adminService.updateTaxConfig(admin.getUserId(), tradeType, request);
    }

    @PostMapping("/users/{targetUserId}/bulk-tokens")
    public BulkTokenResponse issueBulkToken(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID targetUserId, @Valid @RequestBody AdminTokenIssueRequest request) {
        AuthPrincipal admin = authTokenService.require(authorization);
        return adminService.issueBulkToken(admin.getUserId(), targetUserId, request);
    }

    @GetMapping("/users/{targetUserId}/assets")
    public AdminUserAssetResponse userAssets(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID targetUserId) {
        AuthPrincipal admin = authTokenService.require(authorization);
        return adminService.userAssets(admin.getUserId(), targetUserId);
    }

    @GetMapping("/users/{targetUserId}/trades")
    public List<AdminTradeRecordResponse> userTrades(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID targetUserId) {
        AuthPrincipal admin = authTokenService.require(authorization);
        return adminService.userTrades(admin.getUserId(), targetUserId);
    }
}
