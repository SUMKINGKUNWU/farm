package com.farm.exchange.bulk;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/bulk-tokens")
public class BulkTokenController {

    private final BulkTokenService bulkTokenService;

    public BulkTokenController(BulkTokenService bulkTokenService) {
        this.bulkTokenService = bulkTokenService;
    }

    @GetMapping
    public List<BulkTokenResponse> list(@PathVariable UUID userId) {
        return bulkTokenService.list(userId);
    }

    @PostMapping
    public BulkTokenResponse issue(@PathVariable UUID userId, @Valid @RequestBody BulkTokenRequest request) {
        return bulkTokenService.issue(userId, request);
    }
}
