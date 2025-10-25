package com.ecommerce.order.service;

import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.dto.request.CreateOrderRequest;
import com.ecommerce.order.model.dto.response.OrderResponse;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.enums.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderResponse testOrderResponse;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        
        testOrder = Order.builder()
                .orderId(orderId)
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test Customer")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .currency("USD")
                .build();

        testOrderResponse = OrderResponse.builder()
                .orderId(orderId)
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test Customer")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .currency("USD")
                .build();
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrder() {
        // Given
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toOrderResponse(testOrder)).thenReturn(testOrderResponse);

        // When
        OrderResponse result = orderService.getOrderById(orderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo("CUST001");
        verify(orderRepository, times(1)).findByIdWithItems(orderId);
        verify(orderMapper, times(1)).toOrderResponse(testOrder);
    }

    @Test
    void getOrderById_WhenOrderNotFound_ShouldThrowException() {
        // Given
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with ID: " + orderId);
        
        verify(orderRepository, times(1)).findByIdWithItems(orderId);
        verify(orderMapper, never()).toOrderResponse(any());
    }

    @Test
    void cancelOrder_WhenOrderCanBeCancelled_ShouldCancelSuccessfully() {
        // Given
        String reason = "Customer request";
        String cancelledBy = "user@example.com";
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toOrderResponse(testOrder)).thenReturn(testOrderResponse);

        // When
        OrderResponse result = orderService.cancelOrder(orderId, reason, cancelledBy);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(testOrder);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}