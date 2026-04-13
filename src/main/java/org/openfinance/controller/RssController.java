package org.openfinance.controller;

import java.util.List;

import org.openfinance.dto.RssFeedItem;
import org.openfinance.service.RssService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/rss")
@RequiredArgsConstructor
public class RssController {

    private final RssService rssService;

    @GetMapping("/finance")
    public ResponseEntity<List<RssFeedItem>> getFinanceFeeds(Locale locale) {
        return ResponseEntity.ok(rssService.getFinanceFeeds(locale));
    }
}
