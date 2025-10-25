package com.ecommerce.order.model.entity;

import com.ecommerce.order.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_customer_id", columnList = "customer_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at"),
    @Index(name = "idx_order_status_created", columnList = "status,created_at")
})
@Getter
@Setter
@ToString(exclude = {"items"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Business methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean canBeCancelled() {
        return status != OrderStatus.PENDING && status != OrderStatus.PROCESSING;
    }

    public void cancel(String reason, String cancelledBy) {
        if (canBeCancelled()) {
            throw new IllegalStateException(
                String.format("Order in %s status cannot be cancelled", status)
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledReason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
    }

    public void updateStatus(OrderStatus newStatus) {
        // Validate status transition
        if (!isValidStatusTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", this.status, newStatus)
            );
        }

        this.status = newStatus;
        
        // Update timestamps based on status
        switch (newStatus) {
            case PROCESSING -> this.processedAt = LocalDateTime.now();
            case SHIPPED -> this.shippedAt = LocalDateTime.now();
            case DELIVERED -> this.deliveredAt = LocalDateTime.now();
        }
    }

    private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (totalAmount == null) {
            recalculateTotal();
        }
    }
}