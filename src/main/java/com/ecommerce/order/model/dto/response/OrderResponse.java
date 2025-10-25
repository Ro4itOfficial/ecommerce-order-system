package com.ecommerce.order.model.dto.response;

import com.ecommerce.order.model.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Order response object")
public class OrderResponse {

    @Schema(description = "Unique identifier of the order", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID orderId;

    @Schema(description = "Customer ID", example = "CUST123")
    private String customerId;

    @Schema(description = "Customer email", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;

    @Schema(description = "Order items")
    private List<OrderItemResponse> items;

    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Total order amount", example = "1099.99")
    private BigDecimal totalAmount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Shipping address", example = "123 Main St, City, State 12345")
    private String shippingAddress;

    @Schema(description = "Billing address", example = "123 Main St, City, State 12345")
    private String billingAddress;

    @Schema(description = "Order notes", example = "Please deliver before 5 PM")
    private String notes;

    @Schema(description = "Tracking number", example = "TRK123456789")
    private String trackingNumber;

    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Payment status", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Cancellation reason", example = "Customer requested cancellation")
    private String cancelledReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Cancellation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime cancelledAt;

    @Schema(description = "User who cancelled the order", example = "admin@example.com")
    private String cancelledBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Processing start timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime processedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Shipping timestamp", example = "2024-01-15T14:30:00")
    private LocalDateTime shippedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Delivery timestamp", example = "2024-01-17T16:30:00")
    private LocalDateTime deliveredAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Order creation timestamp", example = "2024-01-15T09:30:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Order item response")
    public static class OrderItemResponse {
        
        @Schema(description = "Item ID", example = "550e8400-e29b-41d4-a716-446655440001")
        private UUID itemId;

        @Schema(description = "Product ID", example = "PROD001")
        private String productId;

        @Schema(description = "Product name", example = "Laptop")
        private String productName;

        @Schema(description = "Product description", example = "High-performance laptop")
        private String productDescription;

        @Schema(description = "Product SKU", example = "LAP-001-BLK")
        private String productSku;

        @Schema(description = "Quantity", example = "2")
        private Integer quantity;

        @Schema(description = "Unit price", example = "999.99")
        private BigDecimal unitPrice;

        @Schema(description = "Discount amount", example = "50.00")
        private BigDecimal discountAmount;

        @Schema(description = "Tax amount", example = "99.99")
        private BigDecimal taxAmount;

        @Schema(description = "Subtotal", example = "2049.98")
        private BigDecimal subtotal;

        @Schema(description = "Item notes", example = "Gift wrap requested")
        private String notes;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Item creation timestamp", example = "2024-01-15T09:30:00")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Item update timestamp", example = "2024-01-15T10:30:00")
        private LocalDateTime updatedAt;
    }
}