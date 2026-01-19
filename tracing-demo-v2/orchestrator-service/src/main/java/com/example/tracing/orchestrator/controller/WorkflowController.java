package com.example.tracing.orchestrator.controller;

import com.example.tracing.orchestrator.dto.ProductWorkflowRequest;
import com.example.tracing.orchestrator.dto.ProductWorkflowResponse;
import com.example.tracing.orchestrator.service.ProductWorkflowService;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for orchestrating workflows that demonstrate distributed tracing
 * across HTTP and RabbitMQ communication protocols.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    
    private final ProductWorkflowService workflowService;
    
    public WorkflowController(ProductWorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    
    /**
     * Execute a complete product workflow that demonstrates distributed tracing:
     * 1. Creates a product via HTTP REST call to cqrs-service
     * 2. Updates price via RabbitMQ message to cqrs-service
     * 3. Updates stock via RabbitMQ message to cqrs-service
     * 4. Queries the product via HTTP REST call to cqrs-service
     * 
     * All operations share the same trace ID, allowing you to observe the complete
     * request lifecycle across different communication protocols in your tracing backend.
     * 
     * Example request:
     * POST /api/workflows/product
     * {
     *   "name": "Gaming Laptop",
     *   "description": "High-performance laptop",
     *   "price": 2499.99,
     *   "initialStock": 10,
     *   "updatedPrice": 2299.99,
     *   "updatedStock": 15
     * }
     */
    @PostMapping("/product")
    @Observed(name = "api.workflow.product", contextualName = "api-workflow-product")
    public ResponseEntity<ProductWorkflowResponse> executeProductWorkflow(
            @RequestBody ProductWorkflowRequest request) {
        
        log.info("Received product workflow request: name={}", request.getName());
        
        ProductWorkflowResponse response = workflowService.executeProductWorkflow(request);
        
        log.info("Product workflow completed: productId={}, traceId={}", 
                response.getProductId(), response.getTraceId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Orchestrator service is running");
    }
}
