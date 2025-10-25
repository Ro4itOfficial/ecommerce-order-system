# E-Commerce Order Processing System - API Documentation

## Overview

This document provides comprehensive API documentation for the E-Commerce Order Processing System. The API follows RESTful principles and uses JWT for authentication.

## Base URL

```
http://localhost:8080/api
```

## Authentication

Most endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## API Endpoints

### Authentication Endpoints

#### 1. Register User
- **POST** `/auth/register`
- **Description**: Creates a new user account
- **Request Body**:
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "firstName": "Test",
  "lastName": "User",
  "phoneNumber": "+1234567890",
  "termsAccepted": true
}
```
- **Response**: 201 Created
```json
{
  "message": "User registered successfully. Please verify your email.",
  "success": true,
  "code": 201,
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 2. Login
- **POST** `/auth/login`
- **Description**: Authenticates user and returns JWT tokens
- **Request Body**:
```json
{
  "username": "testuser",
  "password": "Password123!",
  "rememberMe": false
}
```
- **Response**: 200 OK
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshExpiresIn": 86400,
  "username": "testuser",
  "email": "test@example.com",
  "fullName": "Test User",
  "roles": ["USER"],
  "issuedAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-01-15T11:30:00"
}
```

#### 3. Refresh Token
- **POST** `/auth/refresh`
- **Description**: Uses refresh token to generate new access token
- **Headers**:
```
Refresh-Token: <your-refresh-token>
```
- **Response**: 200 OK (Same as login response)

#### 4. Logout
- **POST** `/auth/logout`
- **Description**: Invalidates user tokens
- **Headers**: Requires authentication
- **Response**: 200 OK
```json
{
  "message": "Logged out successfully",
  "success": true
}
```

### Order Endpoints

#### 1. Create Order
- **POST** `/v1/orders`
- **Description**: Creates a new order
- **Headers**: Requires authentication
- **Request Body**:
```json
{
  "customerId": "CUST123",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "items": [
    {
      "productId": "PROD001",
      "productName": "Laptop",
      "productDescription": "High-performance laptop",
      "quantity": 1,
      "unitPrice": 999.99,
      "discountAmount": 0,
      "taxAmount": 99.99
    }
  ],
  "shippingAddress": "123 Main St, City, State 12345",
  "billingAddress": "123 Main St, City, State 12345",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Please deliver before 5 PM",
  "currency": "USD"
}
```
- **Response**: 201 Created
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 1099.98,
  "createdAt": "2024-01-15T10:30:00",
  "items": [...]
}
```

#### 2. Get Order by ID
- **GET** `/v1/orders/{orderId}`
- **Description**: Retrieves order details by ID
- **Headers**: Requires authentication
- **Path Parameters**:
    - `orderId` (UUID): Order identifier
- **Response**: 200 OK (Order details)

#### 3. List Orders
- **GET** `/v1/orders`
- **Description**: Retrieves paginated list of orders
- **Headers**: Requires authentication (Admin only)
- **Query Parameters**:
    - `page` (int): Page number (0-based, default: 0)
    - `size` (int): Page size (default: 20)
    - `sortBy` (string): Sort field (default: createdAt)
    - `sortDirection` (string): Sort direction (ASC/DESC, default: DESC)
- **Response**: 200 OK (Paginated order list)

#### 4. Get Orders by Customer
- **GET** `/v1/orders/customer/{customerId}`
- **Description**: Retrieves orders for a specific customer
- **Headers**: Requires authentication
- **Path Parameters**:
    - `customerId` (string): Customer identifier
- **Query Parameters**:
    - `page` (int): Page number
    - `size` (int): Page size
- **Response**: 200 OK (Paginated order list)

#### 5. Get Orders by Status
- **GET** `/v1/orders/status/{status}`
- **Description**: Retrieves orders with specific status
- **Headers**: Requires authentication
- **Path Parameters**:
    - `status` (string): Order status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
- **Query Parameters**:
    - `page` (int): Page number
    - `size` (int): Page size
- **Response**: 200 OK (Paginated order list)

#### 6. Update Order Status
- **PATCH** `/v1/orders/{orderId}/status`
- **Description**: Updates order status
- **Headers**: Requires authentication (Admin only)
- **Path Parameters**:
    - `orderId` (UUID): Order identifier
- **Request Body**:
```json
{
  "status": "PROCESSING",
  "trackingNumber": "TRK123456789",
  "notes": "Order is being prepared"
}
```
- **Response**: 200 OK (Updated order details)

#### 7. Cancel Order
- **POST** `/v1/orders/{orderId}/cancel`
- **Description**: Cancels an order
- **Headers**: Requires authentication
- **Path Parameters**:
    - `orderId` (UUID): Order identifier
- **Query Parameters**:
    - `reason` (string): Cancellation reason
- **Response**: 200 OK (Cancelled order details)

#### 8. Search Orders
- **GET** `/v1/orders/search`
- **Description**: Search orders with multiple criteria
- **Headers**: Requires authentication
- **Query Parameters**:
    - `customerId` (string): Customer ID
    - `status` (string): Order status
    - `startDate` (string): Start date (YYYY-MM-DD)
    - `endDate` (string): End date (YYYY-MM-DD)
    - `minAmount` (double): Minimum amount
    - `maxAmount` (double): Maximum amount
    - `page` (int): Page number
    - `size` (int): Page size
- **Response**: 200 OK (Paginated search results)

#### 9. Get Order Statistics
- **GET** `/v1/orders/statistics/{customerId}`
- **Description**: Retrieves order statistics for a customer
- **Headers**: Requires authentication
- **Path Parameters**:
    - `customerId` (string): Customer identifier
- **Response**: 200 OK
```json
{
  "totalOrders": 25,
  "pendingOrders": 2,
  "processingOrders": 1,
  "shippedOrders": 3,
  "deliveredOrders": 18,
  "cancelledOrders": 1,
  "totalAmount": 12599.50,
  "averageAmount": 503.98
}
```

## Error Responses

All error responses follow this format:
```json
{
  "message": "Error description",
  "success": false,
  "code": 400,
  "timestamp": "2024-01-15T10:30:00",
  "data": {
    "field": "Validation error message"
  }
}
```

### Common Error Codes

- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Missing or invalid authentication
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error

## Rate Limiting

The API implements rate limiting to prevent abuse:

- **Default limit**: 100 requests per minute per user/IP
- **Headers returned**:
    - `X-RateLimit-Limit`: Maximum requests allowed
    - `X-RateLimit-Window`: Time window
    - `X-RateLimit-Retry-After`: Time until limit resets

## Health Check

### Health Endpoint
- **GET** `/actuator/health`
- **Description**: Returns application health status
- **Response**: 200 OK
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

## Swagger UI

Interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

## Postman Collection

Import the Postman collection from:
```
docs/postman/Order-System.postman_collection.json
```

## cURL Examples

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Password123!"}'
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "items": [{
      "productId": "PROD001",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 999.99
    }]
  }'
```

### Get Order
```bash
curl -X GET http://localhost:8080/api/v1/orders/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

## WebSocket Support (Future)

WebSocket endpoints for real-time updates will be available at:
```
ws://localhost:8080/ws
```

## API Versioning

The API uses URL versioning. Current version: v1
- Base URL: `/api/v1/`
- Previous versions will be maintained for backward compatibility

## Best Practices

1. **Always use HTTPS in production**
2. **Include proper error handling**
3. **Implement retry logic with exponential backoff**
4. **Cache responses when appropriate**
5. **Use pagination for list endpoints**
6. **Validate input data before sending**
7. **Store tokens securely**
8. **Refresh tokens before expiration**

## Support

For API support, contact:
- Email: rohitnegi50@gmail.com