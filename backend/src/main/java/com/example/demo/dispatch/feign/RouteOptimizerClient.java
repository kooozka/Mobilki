package com.example.demo.dispatch.feign;

import com.example.demo.dispatch.dto.feign.AutoPlanOptimizerRequest;
import com.example.demo.dispatch.dto.feign.AutoPlanOptimizerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "route-optimizer", url = "${route.optimizer.url}/api/optimizer/")
public interface RouteOptimizerClient {

    @PostMapping
    AutoPlanOptimizerResponse optimize(@RequestBody AutoPlanOptimizerRequest request);

    @GetMapping("{id}")
    AutoPlanOptimizerResponse getOptimizationResult(@PathVariable Long id);

}
