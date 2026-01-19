package com.example.tracing.orchestrator.service;

import com.example.tracing.orchestrator.dto.ProductWorkflowRequest;
import com.example.tracing.orchestrator.dto.ProductWorkflowResponse;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service that orchestrates a business workflow combining HTTP and RabbitMQ calls.
 * This demonstrates how a single trace ID flows through multiple communication protocols.
 */
@Slf4j
@Service
public class ProductWorkflowService {
    
    private final WebClient webClient;
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    
    @Value("${cqrs.service.url}")
    private String cqrsServiceUrl;
    
    @Value("${cqrs.service.rabbitmq.exchange}")
    private String commandsExchange;
    
    @Value("${cqrs.service.rabbitmq.routing-key}")
    private String commandsRoutingKey;
    
    public ProductWorkflowService(
            WebClient webClient,
            RabbitTemplate rabbitTemplate,
            Tracer tracer) {
        this.webClient = webClient;
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
    }
    
    /**
     * Executes a complete product workflow:
     * 1. Create product via HTTP REST API
     * 2. Update price via RabbitMQ
     * 3. Update stock via RabbitMQ
     * 4. Query product via HTTP REST API
     * 
     * All operations share the same trace ID, demonstrating distributed tracing
     * across HTTP and messaging protocols.
     */
    @Observed(name = "workflow.product.complete", contextualName = "workflow-product-complete")
    public ProductWorkflowResponse executeProductWorkflow(ProductWorkflowRequest request) {
        
        String traceId = tracer.currentSpan() != null 
            ? tracer.currentSpan().context().traceId() 
            : "unknown";
        
        log.info("Starting product workflow with traceId: {}", traceId);
        
        // Step 1: Create product via HTTP
        String productId = createProductViaHttp(request);
        log.info("Step 1 completed: Product created via HTTP, productId={}, traceId={}", 
                productId, traceId);
        
        // Small delay to ensure product is created before updates
        sleep(500);
        
        // Step 2: Update price via RabbitMQ
        updatePriceViaRabbitMQ(productId, request.getUpdatedPrice());
        log.info("Step 2 completed: Price update sent via RabbitMQ, productId={}, traceId={}", 
                productId, traceId);
        
        // Small delay to ensure sequential processing
        sleep(500);
        
        // Step 3: Update stock via RabbitMQ
        updateStockViaRabbitMQ(productId, request.getUpdatedStock());
        log.info("Step 3 completed: Stock update sent via RabbitMQ, productId={}, traceId={}", 
                productId, traceId);
        
        // Delay to allow async processing
        sleep(1000);
        
        // Step 4: Query product via HTTP to verify all changes
        Map<String, Object> product = queryProductViaHttp(productId);
        log.info("Step 4 completed: Product queried via HTTP, productId={}, traceId={}", 
                productId, traceId);
        
        log.info("Product workflow completed successfully, traceId={}", traceId);
        
        return ProductWorkflowResponse.builder()
                .productId(productId)
                .message("Workflow completed successfully")
                .traceId(traceId)
                .steps(ProductWorkflowResponse.WorkflowSteps.builder()
                        .step1CreateViaHttp("Created product via HTTP REST API")
                        .step2UpdatePriceViaRabbitMQ("Updated price via RabbitMQ message")
                        .step3UpdateStockViaRabbitMQ("Updated stock via RabbitMQ message")
                        .step4QueryViaHttp("Queried product via HTTP REST API")
                        .build())
                .build();
    }
    
    /**
     * Step 1: Create product via HTTP REST API.
     * Trace context is automatically propagated via HTTP headers.
     */
    @Observed(name = "workflow.step.create.http", contextualName = "workflow-step-create-http")
    private String createProductViaHttp(ProductWorkflowRequest request) {
        log.info("Creating product via HTTP: name={}", request.getName());
        
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("name", request.getName());
        createRequest.put("description", request.getDescription());
        createRequest.put("price", request.getPrice());
        createRequest.put("initialStock", request.getInitialStock());
        
        Map<String, Object> response = webClient.post()
                .uri(cqrsServiceUrl + "/api/products")
                .bodyValue(createRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        return (String) response.get("productId");
    }
    
    /**
     * Step 2: Update price via RabbitMQ.
     * Trace context is automatically propagated via message headers.
     */
    @Observed(name = "workflow.step.update.price.rabbitmq", contextualName = "workflow-step-update-price-rabbitmq")
    private void updatePriceViaRabbitMQ(String productId, BigDecimal newPrice) {
        log.info("Sending price update via RabbitMQ: productId={}, newPrice={}", productId, newPrice);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("newPrice", newPrice);
        
        Map<String, Object> message = new HashMap<>();
        message.put("commandType", "UpdatePrice");
        message.put("payload", payload);
        
        rabbitTemplate.convertAndSend(commandsExchange, commandsRoutingKey, message);
        
        log.info("Price update message sent to RabbitMQ");
    }
    
    /**
     * Step 3: Update stock via RabbitMQ.
     * Trace context is automatically propagated via message headers.
     */
    @Observed(name = "workflow.step.update.stock.rabbitmq", contextualName = "workflow-step-update-stock-rabbitmq")
    private void updateStockViaRabbitMQ(String productId, Integer quantity) {
        log.info("Sending stock update via RabbitMQ: productId={}, quantity={}", productId, quantity);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("quantity", quantity);
        
        Map<String, Object> message = new HashMap<>();
        message.put("commandType", "UpdateStock");
        message.put("payload", payload);
        
        rabbitTemplate.convertAndSend(commandsExchange, commandsRoutingKey, message);
        
        log.info("Stock update message sent to RabbitMQ");
    }
    
    /**
     * Step 4: Query product via HTTP REST API.
     * Trace context is automatically propagated via HTTP headers.
     */
    @Observed(name = "workflow.step.query.http", contextualName = "workflow-step-query-http")
    private Map<String, Object> queryProductViaHttp(String productId) {
        log.info("Querying product via HTTP: productId={}", productId);
        
        Map<String, Object> product = webClient.get()
                .uri(cqrsServiceUrl + "/api/products/" + productId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        log.info("Product retrieved: {}", product);
        return product;
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
