package org.openfinance.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openfinance.dto.AssetFinancingLink;
import org.openfinance.entity.User;
import org.openfinance.service.AssetFinancingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssetFinancingController {
    private final AssetFinancingService service;

    @GetMapping("/liabilities/{id}/asset-links")
    public List<AssetFinancingLink> forLiability(@PathVariable Long id, Authentication auth) {
        return service.forLiability(((User) auth.getPrincipal()).getId(), id);
    }

    @GetMapping("/assets/{id}/liabilities")
    public List<AssetFinancingLink> forAsset(@PathVariable Long id, Authentication auth) {
        return service.forAsset(((User) auth.getPrincipal()).getId(), id);
    }

    @PutMapping("/liabilities/{id}/asset-links")
    public List<AssetFinancingLink> replace(
            @PathVariable Long id,
            @Valid @RequestBody List<@Valid AssetFinancingLink> links,
            Authentication auth) {
        return service.replace(((User) auth.getPrincipal()).getId(), id, links);
    }
}
