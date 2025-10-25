package com.ecommerce.order.service.impl;

import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.dto.request.CreateOrderRequest;
import com.ecommerce.order.model.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.order.model.dto.response.OrderResponse;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import com.ecommerce.order.model.enums.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    @Override
    @Transactional
    @CacheEvict(value = {"orders", "order-statistics"}, allEntries = true)
    @CircuitBreaker(name = "order-service", fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(CreateOrderRequest request, String userId) {
        log.info("Creating new order for customer: {}", request.getCustomerId());
        
        // Create order entity
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .status(OrderStatus.PENDING)
                .currency(request.getCurrency())
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();
        
        // Add order items
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItem item = OrderItem.builder()
                            .productId(itemRequest.getProductId())
                            .productName(itemRequest.getProductName())
                            .productDescription(itemRequest.getProductDescription())
                            .productSku(itemRequest.getProductSku())
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(itemRequest.getUnitPrice())
                            .discountAmount(itemRequest.getDiscountAmount())
                            .taxAmount(itemRequest.getTaxAmount())
                            .notes(itemRequest.getNotes())
                            .build();
                    item.calculateSubtotal();
                    item.setOrder(order);
                    return item;
                })
                .collect(Collectors.toList());
        
        order.setItems(orderItems);
        order.recalculateTotal();
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully with ID: {}", savedOrder.getOrderId());
        
        // TODO: Publish order created event
        // eventPublisher.publishOrderCreatedEvent(savedOrder);
        
        return orderMapper.toOrderResponse(savedOrder);
    }
    
    @Override
    @Cacheable(value = "orders", key = "#orderId")
    @Retry(name = "order-service")
    public OrderResponse getOrderById(UUID orderId) {
        log.debug("Fetching order with ID: {}", orderId);
        
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        return orderMapper.toOrderResponse(order);
    }
    
    @Override
    @Cacheable(value = "orders", key = "#orderId + '-' + #customerId")
    public OrderResponse getOrderByIdForCustomer(UUID orderId, String customerId) {
        log.debug("Fetching order {} for customer {}", orderId, customerId);
        
        if (!orderRepository.existsByOrderIdAndCustomerId(orderId, customerId)) {
            throw new OrderNotFoundException("Order not found for customer");
        }
        
        return getOrderById(orderId);
    }
    
    @Override
    @Cacheable(value = "order-search", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination: {}", pageable);
        
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toOrderResponse);
    }
    
    @Override
    @Cacheable(value = "order-search", key = "'customer-' + #customerId + '-' + #pageable.pageNumber")
    public Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable) {
        log.debug("Fetching orders for customer: {}", customerId);
        
        Page<Order> orders = orderRepository.findByCustomerId(customerId, pageable);
        return orders.map(orderMapper::toOrderResponse);
    }
    
    @Override
    @Cacheable(value = "order-search", key = "'status-' + #status + '-' + #pageable.pageNumber")
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Fetching orders with status: {}", status);
        
        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        return orders.map(orderMapper::toOrderResponse);
    }
    
    @Override
    @Transactional
    @CachePut(value = "orders", key = "#orderId")
    @CacheEvict(value = {"order-search", "order-statistics"}, allEntries = true)
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", orderId, request.getStatus());
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        OrderStatus newStatus = OrderStatus.fromString(request.getStatus());
        
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(
                String.format("Cannot transition from %s to %s", order.getStatus(), newStatus)
            );
        }
        
        order.updateStatus(newStatus);
        
        if (request.getTrackingNumber() != null && newStatus == OrderStatus.SHIPPED) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated to {}", orderId, newStatus);
        
        // TODO: Publish status change event
        // eventPublisher.publishOrderStatusChangedEvent(updatedOrder);
        
        return orderMapper.toOrderResponse(updatedOrder);
    }
    
    @Override
    @Transactional
    @CachePut(value = "orders", key = "#orderId")
    @CacheEvict(value = {"order-search", "order-statistics"}, allEntries = true)
    public OrderResponse cancelOrder(UUID orderId, String reason, String cancelledBy) {
        log.info("Cancelling order {} by {}", orderId, cancelledBy);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        if (!order.canBeCancelled()) {
            throw new InvalidOrderStateException(
                String.format("Order in %s status cannot be cancelled", order.getStatus())
            );
        }
        
        order.cancel(reason, cancelledBy);
        Order cancelledOrder = orderRepository.save(order);
        
        log.info("Order {} cancelled successfully", orderId);
        
        // TODO: Publish order cancelled event
        // eventPublisher.publishOrderCancelledEvent(cancelledOrder);
        
        return orderMapper.toOrderResponse(cancelledOrder);
    }
    
    @Override
    public Page<OrderResponse> searchOrders(String customerId, OrderStatus status, 
                                           String startDate, String endDate, 
                                           Double minAmount, Double maxAmount, 
                                           Pageable pageable) {
        log.debug("Searching orders with criteria - customerId: {}, status: {}", customerId, status);
        
        LocalDateTime startDateTime = startDate != null ? 
            LocalDateTime.parse(startDate + "T00:00:00") : null;
        LocalDateTime endDateTime = endDate != null ? 
            LocalDateTime.parse(endDate + "T23:59:59") : null;
        BigDecimal minBigDecimal = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
        BigDecimal maxBigDecimal = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;
        
        Page<Order> orders = orderRepository.searchOrders(
            customerId, status, startDateTime, endDateTime, 
            minBigDecimal, maxBigDecimal, pageable
        );
        
        return orders.map(orderMapper::toOrderResponse);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"orders", "order-search", "order-statistics"}, allEntries = true)
    public void processPendingOrders() {
        log.info("Starting batch processing of pending orders");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<Order> pendingOrders = orderRepository.findOrdersForStatusUpdate(cutoffTime, 100);
        
        if (pendingOrders.isEmpty()) {
            log.info("No pending orders to process");
            return;
        }
        
        List<UUID> orderIds = pendingOrders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());
        
        int updatedCount = orderRepository.updateOrderStatusBatch(
            orderIds, OrderStatus.PROCESSING, LocalDateTime.now()
        );
        
        log.info("Updated {} pending orders to PROCESSING status", updatedCount);
        
        // TODO: Send notifications for processed orders
    }
    
    @Override
    @Cacheable(value = "order-statistics", key = "#customerId")
    public Map<String, Object> getOrderStatistics(String customerId) {
        log.debug("Fetching order statistics for customer: {}", customerId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Get basic counts
        long totalOrders = orderRepository.countByCustomerId(customerId);
        statistics.put("totalOrders", totalOrders);
        
        // Get order counts by status
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.findByCustomerIdAndStatus(customerId, status, 
                Pageable.unpaged()).getTotalElements();
            statistics.put(status.name().toLowerCase() + "Orders", count);
        }
        
        // Get aggregated statistics
        List<Object[]> aggregatedStats = orderRepository.getOrderStatisticsByCustomer(customerId);
        if (!aggregatedStats.isEmpty()) {
            Object[] stats = aggregatedStats.get(0);
            statistics.put("totalAmount", stats[2]);
            statistics.put("averageAmount", stats[3]);
        }
        
        return statistics;
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"orders", "order-search", "order-statistics"}, allEntries = true)
    public int deleteOldCancelledOrders(int daysOld) {
        log.info("Deleting cancelled orders older than {} days", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = orderRepository.deleteOldCancelledOrders(cutoffDate);
        
        log.info("Deleted {} old cancelled orders", deletedCount);
        return deletedCount;
    }
    
    // Fallback methods for Circuit Breaker
    public OrderResponse createOrderFallback(CreateOrderRequest request, String userId, Exception ex) {
        log.error("Fallback triggered for createOrder due to: {}", ex.getMessage());
        
        // Return a basic response indicating the service is temporarily unavailable
        return OrderResponse.builder()
                .status(OrderStatus.PENDING)
                .customerName(request.getCustomerName())
                .notes("Order creation is temporarily unavailable. Please try again later.")
                .build();
    }
}