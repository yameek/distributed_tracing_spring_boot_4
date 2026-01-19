package com.example.tracing.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the product workflow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWorkflowResponse {
    private String productId;
    private String message;
    private String traceId;
    private WorkflowSteps steps;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSteps {
        private String step1CreateViaHttp;
        private String step2UpdatePriceViaRabbitMQ;
        private String step3UpdateStockViaRabbitMQ;
        private String step4QueryViaHttp;
    }
}
