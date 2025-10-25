package com.ecommerce.order.service;

import com.ecommerce.order.model.dto.request.CreateOrderRequest;
import com.ecommerce.order.model.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.order.model.dto.response.OrderResponse;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    
    /**
     * Create a new order
     */
    OrderResponse createOrder(CreateOrderRequest request, String userId);
    
    /**
     * Get order by ID
     */
    OrderResponse getOrderById(UUID orderId);
    
    /**
     * Get order by ID for a specific customer
     */
    OrderResponse getOrderByIdForCustomer(UUID orderId, String customerId);
    
    /**
     * Get all orders with pagination
     */
    Page<OrderResponse> getAllOrders(Pageable pageable);
    
    /**
     * Get orders by customer
     */
    Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable);
    
    /**
     * Get orders by status
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);
    
    /**
     * Update order status
     */
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
    
    /**
     * Cancel an order
     */
    OrderResponse cancelOrder(UUID orderId, String reason, String cancelledBy);
    
    /**
     * Search orders with multiple criteria
     */
    Page<OrderResponse> searchOrders(String customerId, OrderStatus status, 
                                     String startDate, String endDate, 
                                     Double minAmount, Double maxAmount, 
                                     Pageable pageable);
    
    /**
     * Process pending orders (for scheduled job)
     */
    void processPendingOrders();
    
    /**
     * Get order statistics for a customer
     */
    Map<String, Object> getOrderStatistics(String customerId);
    
    /**
     * Delete old cancelled orders (for cleanup job)
     */
    int deleteOldCancelledOrders(int daysOld);
}