package com.ecommerce.order.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update order status")
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$", 
            message = "Status must be one of: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED")
    @Schema(description = "New order status", 
           example = "PROCESSING", 
           allowableValues = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"},
           required = true)
    private String status;

    @Schema(description = "Tracking number (required when status is SHIPPED)", example = "TRK123456789")
    private String trackingNumber;

    @Schema(description = "Notes about the status update", example = "Order is being prepared for shipment")
    private String notes;

    @Schema(description = "Reason for cancellation (required when status is CANCELLED)", 
           example = "Customer requested cancellation")
    private String cancellationReason;
}