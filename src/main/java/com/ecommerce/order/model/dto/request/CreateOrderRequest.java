package com.ecommerce.order.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request object for creating a new order")
public class CreateOrderRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    @Schema(description = "Unique identifier of the customer", example = "CUST123", required = true)
    private String customerId;

    @Email(message = "Invalid email format")
    @Schema(description = "Customer email address", example = "customer@example.com")
    private String customerEmail;

    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    @Schema(description = "Customer full name", example = "John Doe")
    private String customerName;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Schema(description = "List of items in the order", required = true)
    private List<OrderItemRequest> items;

    @Schema(description = "Shipping address for the order", example = "123 Main St, City, State 12345")
    private String shippingAddress;

    @Schema(description = "Billing address for the order", example = "123 Main St, City, State 12345")
    private String billingAddress;

    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    @Schema(description = "Payment method", example = "CREDIT_CARD", allowableValues = {"CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER"})
    private String paymentMethod;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Schema(description = "Additional notes for the order", example = "Please deliver before 5 PM")
    private String notes;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    @Schema(description = "Currency code (ISO 4217)", example = "USD", defaultValue = "USD")
    @Builder.Default
    private String currency = "USD";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Order item details")
    public static class OrderItemRequest {
        
        @NotBlank(message = "Product ID is required")
        @Size(max = 50, message = "Product ID must not exceed 50 characters")
        @Schema(description = "Unique identifier of the product", example = "PROD001", required = true)
        private String productId;

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must not exceed 200 characters")
        @Schema(description = "Name of the product", example = "Laptop", required = true)
        private String productName;

        @Schema(description = "Product description", example = "High-performance laptop with 16GB RAM")
        private String productDescription;

        @Size(max = 50, message = "Product SKU must not exceed 50 characters")
        @Schema(description = "Product SKU", example = "LAP-001-BLK")
        private String productSku;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @Max(value = 9999, message = "Quantity cannot exceed 9999")
        @Schema(description = "Quantity of the product", example = "2", required = true)
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
        @DecimalMax(value = "999999.99", message = "Unit price cannot exceed 999999.99")
        @Schema(description = "Price per unit of the product", example = "999.99", required = true)
        private BigDecimal unitPrice;

        @DecimalMin(value = "0.00", message = "Discount amount cannot be negative")
        @Schema(description = "Discount amount for this item", example = "50.00", defaultValue = "0.00")
        @Builder.Default
        private BigDecimal discountAmount = BigDecimal.ZERO;

        @DecimalMin(value = "0.00", message = "Tax amount cannot be negative")
        @Schema(description = "Tax amount for this item", example = "99.99", defaultValue = "0.00")
        @Builder.Default
        private BigDecimal taxAmount = BigDecimal.ZERO;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        @Schema(description = "Additional notes for this item", example = "Gift wrap requested")
        private String notes;
    }
}