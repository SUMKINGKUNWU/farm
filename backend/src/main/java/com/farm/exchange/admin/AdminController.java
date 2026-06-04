package com.farm.exchange.admin;

import com.farm.exchange.bulk.BulkTokenResponse;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/tax-configs")
    public List<TaxConfigResponse> taxConfigs(@RequestParam UUID adminUserId) {
        return adminService.taxConfigs(adminUserId);
    }

    @PutMapping("/tax-configs/{tradeType}")
    public TaxConfigResponse updateTaxConfig(@RequestParam UUID adminUserId, @PathVariable String tradeType, @Valid @RequestBody TaxConfigRequest request) {
        return adminService.updateTaxConfig(adminUserId, tradeType, request);
    }

    @PostMapping("/users/{targetUserId}/bulk-tokens")
    public BulkTokenResponse issueBulkToken(@RequestParam UUID adminUserId, @PathVariable UUID targetUserId, @Valid @RequestBody AdminTokenIssueRequest request) {
        return adminService.issueBulkToken(adminUserId, targetUserId, request);
    }

    @GetMapping("/users/{targetUserId}/assets")
    public AdminUserAssetResponse userAssets(@RequestParam UUID adminUserId, @PathVariable UUID targetUserId) {
        return adminService.userAssets(adminUserId, targetUserId);
    }

    @GetMapping("/users/{targetUserId}/trades")
    public List<AdminTradeRecordResponse> userTrades(@RequestParam UUID adminUserId, @PathVariable UUID targetUserId) {
        return adminService.userTrades(adminUserId, targetUserId);
    }
}
