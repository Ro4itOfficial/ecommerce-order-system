package com.ecommerce.order.repository;

import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    // Find orders by customer ID
    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    // Find orders by status
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Find orders by customer ID and status
    Page<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);

    // Find orders with items (avoiding N+1 problem)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    // Find all orders with items for a customer
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId")
    List<Order> findByCustomerIdWithItems(@Param("customerId") String customerId);

    // Find pending orders older than specified time
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :cutoffTime")
    List<Order> findPendingOrdersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Batch update status for multiple orders
    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus, o.updatedAt = :updatedAt WHERE o.orderId IN :orderIds")
    int updateOrderStatusBatch(@Param("orderIds") List<UUID> orderIds,
                               @Param("newStatus") OrderStatus newStatus,
                               @Param("updatedAt") LocalDateTime updatedAt);

    // Find orders created between dates
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Count orders by status
    long countByStatus(OrderStatus status);

    // Count orders by customer
    long countByCustomerId(String customerId);

    // Find orders with total amount greater than
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount")
    Page<Order> findOrdersWithTotalAmountGreaterThan(@Param("amount") BigDecimal amount, Pageable pageable);

    // Get order statistics by customer
    @Query("""
        SELECT o.customerId, COUNT(o), SUM(o.totalAmount), AVG(o.totalAmount) 
        FROM Order o 
        WHERE o.customerId = :customerId 
        GROUP BY o.customerId
        """)
    List<Object[]> getOrderStatisticsByCustomer(@Param("customerId") String customerId);

    // Find orders for status update job
    @Query(value = """
        SELECT o.* FROM orders o 
        WHERE o.status = 'PENDING' 
        AND o.created_at < :cutoffTime 
        ORDER BY o.created_at ASC 
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Order> findOrdersForStatusUpdate(@Param("cutoffTime") LocalDateTime cutoffTime,
                                          @Param("batchSize") int batchSize);

    // Check if order exists for customer
    boolean existsByOrderIdAndCustomerId(UUID orderId, String customerId);

    // Delete old cancelled orders
    @Modifying
    @Query("DELETE FROM Order o WHERE o.status = 'CANCELLED' AND o.cancelledAt < :cutoffDate")
    int deleteOldCancelledOrders(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find orders with specific payment status
    Page<Order> findByPaymentStatus(String paymentStatus, Pageable pageable);

    // Search orders by tracking number
    Optional<Order> findByTrackingNumber(String trackingNumber);

    // Complex search with multiple criteria
    @Query("""
        SELECT o FROM Order o 
        WHERE (:customerId IS NULL OR o.customerId = :customerId)
        AND (:status IS NULL OR o.status = :status)
        AND (:startDate IS NULL OR o.createdAt >= :startDate)
        AND (:endDate IS NULL OR o.createdAt <= :endDate)
        AND (:minAmount IS NULL OR o.totalAmount >= :minAmount)
        AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount)
        """)
    Page<Order> searchOrders(@Param("customerId") String customerId,
                             @Param("status") OrderStatus status,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             @Param("minAmount") BigDecimal minAmount,
                             @Param("maxAmount") BigDecimal maxAmount,
                             Pageable pageable);
}