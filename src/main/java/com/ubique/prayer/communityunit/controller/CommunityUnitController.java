package com.ubique.prayer.communityunit.controller;

import com.ubique.prayer.communityunit.dto.CommunityUnitOptionResponse;
import com.ubique.prayer.communityunit.service.CommunityUnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community-units")
public class CommunityUnitController {

    private static final Logger log =
            LoggerFactory.getLogger(CommunityUnitController.class);
    private final CommunityUnitService communityUnitService;

    public CommunityUnitController(CommunityUnitService communityUnitService) {
        this.communityUnitService = communityUnitService;
    }

    @GetMapping("/cells")
    public ResponseEntity<List<CommunityUnitOptionResponse>> getActiveCells() {
        log.debug("CommunityUnitController.getActiveCells start");

        List<CommunityUnitOptionResponse> responses = communityUnitService.getActiveCells();

        log.debug("CommunityUnitController.getActiveCells end {}", responses);

        return ResponseEntity.ok(responses);
    }
}