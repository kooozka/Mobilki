package com.example.optimizer.controller;

import com.example.optimizer.dto.OptimizerRequest;
import com.example.optimizer.dto.OptimizerResponse;
import com.example.optimizer.service.OptimizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/optimizer")
@RequiredArgsConstructor
public class OptimizerController {

    private final OptimizerService optimizerService;

    @PostMapping
    public ResponseEntity optimize(@RequestBody OptimizerRequest request) {
        optimizerService.optimize(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("{id}")
    ResponseEntity getOptimizationResult(@PathVariable Long id) {
        OptimizerResponse response = optimizerService.getOptimizationResult(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(response);
        }
    }
}
