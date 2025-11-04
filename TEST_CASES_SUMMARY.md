# Booking Service Test Cases Summary

## Overview
Comprehensive test suite for `BookingService` covering various scenarios including concurrent bookings, payment failures, reservation management, and edge cases.

## Test File Location
`src/test/java/com/bms/BasicBookMyShow/service/BookingServiceTest.java`

## Test Cases

### 1. **Basic Booking Tests**

#### `testSuccessfulBooking()`
- Tests successful ticket booking flow
- Verifies booking creation, seat status changes to BOOKED
- Validates booking contains correct user, show, and seats

#### `testMultipleSeatsBooking()`
- Tests booking multiple seats in a single transaction
- Verifies all seats are marked as BOOKED
- Validates booking contains all selected seats

#### `testBookingPriceCalculation()`
- Tests price calculation for bookings
- Verifies total amount is correctly calculated

### 2. **Reservation Tests**

#### `testReserveSeats()`
- Tests seat reservation functionality
- Verifies seats are marked as RESERVED after reservation
- Validates reservation ID is returned

#### `testReservationPreventsDoubleBooking()`
- Tests that reserved seats cannot be reserved by another user
- Verifies reservation release functionality
- Confirms seats become available after release

#### `testTwoPhaseBooking()`
- Tests two-phase booking process (reserve → confirm)
- Verifies seats transition from RESERVED to BOOKED after payment
- Validates reservation is removed after successful booking

#### `testReleaseReservation()`
- Tests manual reservation release
- Verifies seats return to AVAILABLE status after release

#### `testExpiredReservationCanBeReused()`
- Tests that expired/released reservations allow new reservations
- Verifies seat availability after reservation release

#### `testExpiredReservationsTreatedAsAvailable()`
- Tests that expired reservations are automatically treated as available
- Validates lazy cleanup mechanism

### 3. **Concurrency Tests**

#### `testConcurrentBookingSameSeats()`
- Tests multiple threads trying to book the same seat simultaneously
- Verifies only one booking succeeds (race condition prevention)
- Uses 10 concurrent threads for stress testing

#### `testConcurrentBookingDifferentSeats()`
- Tests concurrent bookings of different seats
- Verifies all bookings succeed when seats don't conflict
- Tests fine-grained locking per show

### 4. **Availability Tests**

#### `testAvailableSeatsAfterBooking()`
- Tests seat availability query after bookings
- Verifies booked seats are excluded from available seats
- Validates unbooked seats remain available

#### `testAvailableSeatsCount()`
- Tests accurate count of available seats
- Verifies booked and reserved seats are excluded
- Validates correct calculation: Total - Booked - Reserved = Available

### 5. **Cancellation Tests**

#### `testCancelBooking()`
- Tests booking cancellation
- Verifies booking status changes to CANCELLED
- Validates seats return to AVAILABLE status after cancellation
- Note: Requires booking to be CONFIRMED first

### 6. **Error Handling Tests**

#### `testBookingUnavailableSeats()`
- Tests attempting to book already booked seats
- Verifies reservation fails when seats are unavailable
- Validates no booking is created for unavailable seats

#### `testInvalidReservationId()`
- Tests booking confirmation with invalid reservation ID
- Verifies exception is thrown for invalid reservations

#### `testBookingWithWrongUser()`
- Tests security: user cannot confirm another user's reservation
- Verifies exception is thrown when wrong user tries to confirm

#### `testPartialSeatBooking()`
- Tests attempting to book mix of available and booked seats
- Verifies entire reservation fails if any seat is unavailable
- Validates atomic reservation (all or nothing)

#### `testMultipleSeatsPartialAvailability()`
- Tests reservation attempt with partially available seats
- Verifies reservation fails if any seat is already booked
- Validates available seats remain unchanged after failed reservation

### 7. **Payment Failure Tests**

#### `testPaymentFailureKeepsReservation()`
- Tests payment failure handling
- **Note**: Current payment strategies always succeed (they retry internally)
- Structure is in place for testing actual payment failures
- Verifies seats remain RESERVED if payment fails (when implemented)

#### `testRetryPaymentAfterFailure()`
- Tests retry mechanism after payment failure
- **Note**: Currently succeeds due to payment strategy retry logic
- Structure demonstrates retry flow with different payment methods

## Test Coverage

### Scenarios Covered ✅
- ✅ Basic booking flow
- ✅ Reservation management
- ✅ Concurrent bookings (same seats)
- ✅ Concurrent bookings (different seats)
- ✅ Seat availability queries
- ✅ Booking cancellation
- ✅ Reservation expiry/release
- ✅ Error handling (invalid inputs, wrong users)
- ✅ Partial availability scenarios
- ✅ Two-phase booking process

### Scenarios Not Fully Testable (Due to Current Implementation)
- ⚠️ Actual payment failures (payment strategies always succeed)
- ⚠️ Reservation expiry after 5 minutes (would require time manipulation)

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run specific test method
mvn test -Dtest=BookingServiceTest#testConcurrentBookingSameSeats
```

## Test Dependencies

- JUnit 5 (Jupiter)
- Java 21
- Spring Boot Test (for context loading if needed)

## Test Data Setup

Each test uses `@BeforeEach` to set up:
- Movie instance
- Multiplex instance
- Screen with 90 seats (30 Silver, 30 Gold, 30 Platinum)
- Show instance
- Test users (user1, user2, user3)

## Notes for Future Enhancements

1. **Payment Failure Testing**: Consider creating a test-only payment strategy that can be configured to fail for testing payment failure scenarios

2. **Time-based Testing**: Use libraries like `java.time.Clock` or mocking frameworks to test reservation expiry after 5 minutes

3. **Mocking**: Consider using Mockito for mocking PaymentService to test payment failures

4. **Integration Tests**: Add integration tests that test the full flow including database operations (when database is added)

5. **Performance Tests**: Add performance benchmarks for concurrent booking scenarios

## Example Test Structure

```java
@Test
void testName() {
    // Arrange - Set up test data
    List<Seat> seats = getSeats(screen, "1", "2");
    
    // Act - Perform the action being tested
    Booking booking = bookingService.bookTicket(user, show, seats, PaymentMethod.CREDIT_CARD);
    
    // Assert - Verify the results
    assertNotNull(booking);
    assertEquals(2, booking.getReservedSeats().size());
}
```

## Concurrency Test Pattern

```java
@Test
void testConcurrentScenario() throws InterruptedException {
    int numberOfThreads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    
    // Launch concurrent operations
    // Verify only expected number succeed
}
```

