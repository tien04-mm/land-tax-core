package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.HistoryDTO;
import com.thanglong.landtax.usecase.service.LandHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lands")
@RequiredArgsConstructor
@Slf4j
public class LandHistoryController {

    private final LandHistoryService landHistoryService;

    @GetMapping("/{id}/history")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<HistoryDTO>> getLandHistory(@PathVariable Integer id) {
        log.info("GET /api/lands/{}/history", id);
        List<HistoryDTO> history = landHistoryService.getLandHistory(id);
        return ResponseEntity.ok(history);
    }
}
