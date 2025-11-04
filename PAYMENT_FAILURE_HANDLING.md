# Payment Failure Handling

## Overview
This document describes how the system handles payment failures during seat booking confirmation.

## Problem
Previously, seats were marked as **BOOKED** before payment processing, which caused issues:
- If payment failed, seats were incorrectly marked as BOOKED
- Seats became unavailable even though payment didn't succeed
- No way to retry payment without losing the reservation

## Solution
The system now processes payment **BEFORE** marking seats as BOOKED, ensuring atomicity and proper rollback.

## How It Works

### 1. Payment Processing Order
```java
// OLD (INCORRECT):
markSeatsAsBooked(seats);           // ❌ Seats marked as BOOKED
Payment payment = processPayment(); // Payment may fail

// NEW (CORRECT):
Payment payment = processPayment();  // ✅ Process payment first
if (payment.isSuccessful()) {
    markSeatsAsBooked(seats);        // Only mark as BOOKED if payment succeeds
}
```

### 2. Payment Failure Handling

When payment fails:
- ✅ Seats remain in **RESERVED** status (not BOOKED)
- ✅ Reservation is kept intact
- ✅ User can retry payment with the same reservation
- ✅ `PaymentFailedException` is thrown with reservation details
- ✅ No seats are permanently lost

### 3. Code Flow

```132:203:src/main/java/com/bms/BasicBookMyShow/service/BookingService.java
public Booking confirmBookingWithReservation(User user, String reservationId, PaymentMethod paymentMethod) 
        throws PaymentFailedException {
    // ... validation ...
    
    // Process payment FIRST (before marking seats as BOOKED)
    Payment payment = paymentService.processServicePayment(price);
    
    // Check if payment was successful
    if (payment.getPaymentStatus() != PaymentStatus.SUCCESSFUL) {
        // Payment failed - keep seats as RESERVED and reservation intact for retry
        throw new PaymentFailedException(...);
    }
    
    // Payment successful - proceed with booking
    markSeatsAsBooked(reservedSeats, show);
    // ...
}
```

## Usage Examples

### Basic Booking with Error Handling
```java
try {
    Booking booking = bookingService.confirmBookingWithReservation(
        user, reservationId, PaymentMethod.CREDIT_CARD);
    System.out.println("Booking successful: " + booking.getId());
} catch (PaymentFailedException e) {
    System.out.println("Payment failed: " + e.getMessage());
    System.out.println("Reservation ID: " + e.getReservationId() + " is still active");
    // User can retry payment
}
```

### Retry Payment
```java
String reservationId = bookingService.reserveSeats(user, show, selectedSeats);

try {
    Booking booking = bookingService.confirmBookingWithReservation(
        user, reservationId, PaymentMethod.CREDIT_CARD);
    // Success!
} catch (PaymentFailedException e) {
    // Payment failed - retry with different payment method
    try {
        Booking booking = bookingService.retryPaymentWithReservation(
            user, reservationId, PaymentMethod.UPI);
        // Retry successful!
    } catch (PaymentFailedException e2) {
        // Still failed - user can manually release reservation or wait for expiry
        bookingService.releaseReservation(reservationId);
    }
}
```

### Auto-Release on Failure (Alternative Pattern)
```java
try {
    Booking booking = bookingService.bookTicket(user, show, seats, PaymentMethod.CREDIT_CARD);
} catch (PaymentFailedException e) {
    // Option 1: Keep reservation for user to retry manually
    // Reservation remains active until expiry (5 minutes)
    
    // Option 2: Automatically release reservation
    bookingService.releaseReservation(e.getReservationId());
    
    // Option 3: Retry automatically with different payment method
    try {
        booking = bookingService.retryPaymentWithReservation(
            user, e.getReservationId(), PaymentMethod.UPI);
    } catch (PaymentFailedException e2) {
        bookingService.releaseReservation(e.getReservationId());
    }
}
```

## Payment Failure Scenarios

### Scenario 1: Network/Timeout Error
- Payment service doesn't respond
- Reservation stays active
- User can retry after network issues resolved

### Scenario 2: Insufficient Funds
- Payment status: `FAILED`
- Reservation stays active
- User can try different payment method or account

### Scenario 3: Payment Gateway Error
- Payment status: `FAILED`
- Reservation stays active
- User can retry same payment method (may be transient error)

### Scenario 4: Reservation Expires During Retry
- If user waits too long (>5 minutes), reservation expires
- `confirmBookingWithReservation()` returns `null`
- Seats automatically become available again
- User needs to reserve seats again

## Best Practices

### 1. Handle PaymentFailuredException
Always catch `PaymentFailedException` and handle appropriately:
```java
try {
    booking = bookingService.confirmBookingWithReservation(...);
} catch (PaymentFailedException e) {
    // Log error, notify user, decide on retry strategy
}
```

### 2. Retry Strategy
Consider implementing retry logic:
```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        booking = bookingService.confirmBookingWithReservation(...);
        break; // Success
    } catch (PaymentFailedException e) {
        if (i == maxRetries - 1) {
            // Final attempt failed
            bookingService.releaseReservation(e.getReservationId());
            throw e;
        }
        // Wait before retry
        Thread.sleep(1000);
    }
}
```

### 3. Reservation Timeout Awareness
Remember reservations expire after 5 minutes:
```java
// Check reservation status before retry
SeatReservation reservation = seatLockManager.getReservation(reservationId);
if (reservation != null && !reservation.isExpired()) {
    // Safe to retry
    booking = bookingService.retryPaymentWithReservation(...);
} else {
    // Reservation expired, need to reserve again
    reservationId = bookingService.reserveSeats(user, show, seats);
}
```

### 4. User Notification
Notify users about payment failures and reservation status:
- Inform user that reservation is still active
- Show remaining time until expiry
- Suggest retry options

## Benefits

✅ **Atomic Operations**: Payment and booking are atomic - either both succeed or both fail  
✅ **No Lost Seats**: Failed payments don't result in permanently unavailable seats  
✅ **Retry Capability**: Users can retry payment without losing reservation  
✅ **Better UX**: Clear error messages with actionable options  
✅ **Data Consistency**: Seat status always reflects actual booking state  

## Exception Details

### PaymentFailedException
- **Message**: Describes the failure reason
- **reservationId**: The reservation that failed (still active)
- **paymentId**: The failed payment transaction ID

This allows callers to:
- Log the failed payment
- Notify the user
- Implement retry logic
- Track payment failure rates

