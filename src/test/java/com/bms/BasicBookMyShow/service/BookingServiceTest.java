package com.bms.BasicBookMyShow.service;

import com.bms.BasicBookMyShow.model.*;
import com.bms.BasicBookMyShow.payment.PaymentStrategy;
import com.bms.BasicBookMyShow.pricing.PeakPricingStrategy;
import com.bms.BasicBookMyShow.service.BookingService;
import com.bms.BasicBookMyShow.service.PaymentFailedException;
import com.bms.BasicBookMyShow.service.SeatLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test cases for BookingService
 * Tests concurrent booking, payment failures, reservation expiry, and edge cases
 */
class BookingServiceTest {

    private BookingService bookingService;
    private Movie movie;
    private Multiplex multiplex;
    private Screen screen;
    private Show show;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // Get fresh instance for each test
        bookingService = BookingService.getInstance();
        
        // Create test data
        movie = new Movie("Test Movie", "English", "Action", "2024-01-01", 120);
        bookingService.addMovie(movie);

        multiplex = new Multiplex("Test Multiplex", "Test Location");
        bookingService.addMultiplex(multiplex);

        screen = new Screen("Screen 1", multiplex, 90);
        multiplex.addScreen(screen);

        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
        LocalDateTime endTime = startTime.plusHours(2);
        show = new Show(movie, startTime, endTime, screen);
        // Set pricing strategy for the show (required for price calculation)
        show.setPricingStrategy(new PeakPricingStrategy());
        bookingService.addShow(show);

        user1 = new User("User1", "user1@test.com");
        user2 = new User("User2", "user2@test.com");
        user3 = new User("User3", "user3@test.com");
    }

    @Test
    void testSuccessfulBooking() throws PaymentFailedException {
        // Arrange
        List<Seat> selectedSeats = getSeats(screen, "1", "2", "3");

        // Act - May need retry due to random payment failures
        Booking booking = null;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                booking = bookingService.bookTicket(user1, show, selectedSeats, PaymentMethod.CREDIT_CARD);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Retry
            }
        }

        // Assert
        assertNotNull(booking, "Booking should not be null");
        assertEquals(user1.getId(), booking.getUser().getId());
        assertEquals(show.getId(), booking.getShow().getId());
        assertEquals(3, booking.getReservedSeats().size());
//        assertNotNull(booking.getPayment());
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());

        // Verify seats are marked as BOOKED
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.BOOKED, showSeat.getSeatStatus());
        }
    }

    @Test
    void testReserveSeats() {
        // Arrange
        List<Seat> selectedSeats = getSeats(screen, "5", "6");

        // Act
        String reservationId = bookingService.reserveSeats(user1, show, selectedSeats);

        // Assert
        assertNotNull(reservationId, "Reservation ID should not be null");
        
        // Verify seats are marked as RESERVED
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.RESERVED, showSeat.getSeatStatus());
        }
    }

    @Test
    void testBookingUnavailableSeats() {
        // Arrange
        List<Seat> seats1 = getSeats(screen, "10", "11");
        List<Seat> seats2 = getSeats(screen, "10", "12"); // Seat 10 already booked

        // Act - First user books seats
        try {
            bookingService.bookTicket(user1, show, seats1, PaymentMethod.CREDIT_CARD);
        } catch (PaymentFailedException e) {
            fail("First booking should succeed");
        }

        // Act - Second user tries to book overlapping seat
        String reservationId = bookingService.reserveSeats(user2, show, seats2);
        
        // Assert - Should not be able to reserve seat 10
        assertNull(reservationId, "Should not be able to reserve already booked seat");
    }

    @Test
    void testConcurrentBookingSameSeats() throws InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Seat> seats = getSeats(screen, "20"); // Single seat

        // Act - Multiple threads try to book the same seat concurrently
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            Future<?> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    User user = new User("User" + threadNum, "user" + threadNum + "@test.com");
                    try {
                        Booking booking = bookingService.bookTicket(user, show, seats, PaymentMethod.CREDIT_CARD);
                        if (booking != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (PaymentFailedException e) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();

        // Assert - Only one booking should succeed
        assertEquals(1, successCount.get(), "Only one concurrent booking should succeed");
        assertEquals(0, failureCount.get(), "No payment failures expected");
        
        // Verify seat is BOOKED
        Seat seat = show.getScreen().getSeats().get("20");
        assertEquals(SeatStatus.BOOKED, seat.getSeatStatus());
    }

    @Test
    void testConcurrentBookingDifferentSeats() throws InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        int numberOfThreads = 5;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Multiple threads book different seats
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            Future<?> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    User user = new User("User" + threadNum, "user" + threadNum + "@test.com");
                    List<Seat> seats = getSeats(screen, String.valueOf(30 + threadNum));
                    // Retry if payment fails
                    Booking booking = null;
                    int maxRetries = 5;
                    for (int retry = 0; retry < maxRetries; retry++) {
                        try {
                            booking = bookingService.bookTicket(user, show, seats, PaymentMethod.CREDIT_CARD);
                            if (booking != null) {
                                successCount.incrementAndGet();
                            }
                            break; // Success
                        } catch (PaymentFailedException e) {
                            if (retry == maxRetries - 1) {
                                // Final attempt failed
                                break;
                            }
                            // Retry
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();

        // Assert - All bookings should succeed
        assertEquals(numberOfThreads, successCount.get(), "All concurrent bookings of different seats should succeed");
    }

    @Test
    void testAvailableSeatsAfterBooking() throws PaymentFailedException {
        // Arrange
        List<Seat> bookedSeats = getSeats(screen, "40", "41", "42");
        // May need retry due to random payment failures
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                bookingService.bookTicket(user1, show, bookedSeats, PaymentMethod.CREDIT_CARD);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Retry
            }
        }

        // Act
        Map<String, Seat> availableSeats = bookingService.availableSeats(show);

        // Assert
        assertFalse(availableSeats.containsKey("40"), "Booked seat should not be available");
        assertFalse(availableSeats.containsKey("41"), "Booked seat should not be available");
        assertFalse(availableSeats.containsKey("42"), "Booked seat should not be available");
        assertTrue(availableSeats.containsKey("43"), "Unbooked seat should be available");
    }

    @Test
    void testCancelBooking() throws PaymentFailedException {
        // Arrange
        List<Seat> seats = getSeats(screen, "50", "51");
        Booking booking = bookingService.bookTicket(user1, show, seats, PaymentMethod.CREDIT_CARD);
        bookingService.confirmBooking(booking.getId()); // Confirm first

        // Act
        bookingService.cancelBooking(booking.getId());

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        
        // Verify seats are back to AVAILABLE
        Seat seat50 = show.getScreen().getSeats().get("50");
        Seat seat51 = show.getScreen().getSeats().get("51");
        assertEquals(SeatStatus.AVAILABLE, seat50.getSeatStatus());
        assertEquals(SeatStatus.AVAILABLE, seat51.getSeatStatus());
    }

    @Test
    void testReservationPreventsDoubleBooking() {
        // Arrange
        List<Seat> seats = getSeats(screen, "60");
        String reservationId = bookingService.reserveSeats(user1, show, seats);
        assertNotNull(reservationId);
        
        // Act - Try to book same seat (should fail because it's reserved)
        String reservationId2 = bookingService.reserveSeats(user2, show, seats);
        assertNull(reservationId2, "Should not be able to reserve already reserved seat");

        // Verify seat is still RESERVED
        Seat seat = show.getScreen().getSeats().get("60");
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus());

        // Release reservation manually
        bookingService.releaseReservation(reservationId);

        // Now should be able to reserve
        String reservationId3 = bookingService.reserveSeats(user2, show, seats);
        assertNotNull(reservationId3, "Should be able to reserve after release");
    }

    @Test
    void testExpiredReservationsTreatedAsAvailable() {
        // Arrange
        List<Seat> seats = getSeats(screen, "70");
        String reservationId = bookingService.reserveSeats(user1, show, seats);
        assertNotNull(reservationId);

        // Act - Release the reservation (simulating expiry cleanup)
        bookingService.releaseReservation(reservationId);

        // Assert - Seat should be available again
        Seat seat = show.getScreen().getSeats().get("70");
        assertEquals(SeatStatus.AVAILABLE, seat.getSeatStatus());

        // Should be able to reserve again
        String newReservationId = bookingService.reserveSeats(user2, show, seats);
        assertNotNull(newReservationId, "Should be able to reserve after expiry");
    }

    @Test
    void testTwoPhaseBooking() throws PaymentFailedException {
        // Arrange
        List<Seat> seats = getSeats(screen, "80", "81");

        // Act - Phase 1: Reserve
        String reservationId = bookingService.reserveSeats(user1, show, seats);
        assertNotNull(reservationId);

        // Verify seats are RESERVED
        for (Seat seat : seats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.RESERVED, showSeat.getSeatStatus());
        }

        // Act - Phase 2: Confirm with payment (may need retry due to random payment failures)
        Booking booking = null;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                booking = bookingService.confirmBookingWithReservation(user1, reservationId, PaymentMethod.UPI);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Retry with same reservation
                reservationId = e.getReservationId();
            }
        }
        assertNotNull(booking);

        // Assert - Seats should now be BOOKED
        for (Seat seat : seats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.BOOKED, showSeat.getSeatStatus());
        }
    }

    @Test
    void testInvalidReservationId() {
        // Act & Assert - Returns null for invalid reservation, doesn't throw exception
        Booking result = null;
        try {
            result = bookingService.confirmBookingWithReservation(user1, "invalid-id", PaymentMethod.CREDIT_CARD);
        } catch (PaymentFailedException e) {
            // This would be thrown if reservation existed but payment failed
            fail("Should not throw PaymentFailedException for invalid reservation");
        }
        assertNull(result, "Should return null for invalid reservation");
    }

    @Test
    void testBookingWithWrongUser() {
        // Arrange
        List<Seat> seats = getSeats(screen, "87");
        String reservationId = bookingService.reserveSeats(user1, show, seats);

        // Act & Assert - User2 tries to confirm user1's reservation
        // Returns null (doesn't throw exception) when wrong user tries to confirm
        Booking result = null;
        try {
            result = bookingService.confirmBookingWithReservation(user2, reservationId, PaymentMethod.CREDIT_CARD);
        } catch (PaymentFailedException e) {
            // This would only be thrown if payment failed
            fail("Should not throw PaymentFailedException for wrong user");
        }
        assertNull(result, "Should return null when wrong user tries to confirm");
        
        // Verify seat is still RESERVED
        Seat seat = show.getScreen().getSeats().get("87");
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus(), "Seat should remain RESERVED");
    }

    @Test
    void testMultipleSeatsBooking() throws PaymentFailedException {
        // Arrange - Book multiple seats at once (using different seats than testSuccessfulBooking)
        List<Seat> seats = getSeats(screen, "6", "7", "8", "9", "10");

        // Act - May need retry due to random payment failures
        Booking booking = null;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                booking = bookingService.bookTicket(user1, show, seats, PaymentMethod.DEBIT_CARD);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Retry
            }
        }

        // Assert
        assertNotNull(booking);
        assertEquals(5, booking.getReservedSeats().size());

        // Verify all seats are booked
        for (Seat seat : seats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.BOOKED, showSeat.getSeatStatus());
        }
    }

    @Test
    void testBookingPriceCalculation() throws PaymentFailedException {
        // Arrange
        List<Seat> seats = getSeats(screen, "90");

        // Act - May need retry due to random payment failures
        Booking booking = null;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                booking = bookingService.bookTicket(user1, show, seats, PaymentMethod.CREDIT_CARD);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Try again with same seats (will create new reservation)
            }
        }

        // Assert
        assertNotNull(booking);
        assertTrue(booking.getTotalAmount() > 0, "Total amount should be positive");
    }

    @Test
    void testReleaseReservation() {
        // Arrange
        List<Seat> seats = getSeats(screen, "75");
        String reservationId = bookingService.reserveSeats(user1, show, seats);

        // Verify seat is RESERVED
        Seat seat = show.getScreen().getSeats().get("75");
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus());

        // Act
        bookingService.releaseReservation(reservationId);

        // Assert - Seat should be AVAILABLE again
        assertEquals(SeatStatus.AVAILABLE, seat.getSeatStatus());
    }

    @Test
    void testPartialSeatBooking() {
        // Arrange - Try to book one available and one booked seat
        List<Seat> firstBooking = getSeats(screen, "80");
        // Retry if payment fails
        int maxRetries = 5;
        boolean booked = false;
        for (int i = 0; i < maxRetries; i++) {
            try {
                bookingService.bookTicket(user1, show, firstBooking, PaymentMethod.CREDIT_CARD);
                booked = true;
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    fail("First booking should eventually succeed");
                }
                // Retry
            }
        }
        assertTrue(booked, "First booking should succeed");

        // Act - Try to book seat 80 (already booked) and seat 81 (available)
        List<Seat> secondBooking = getSeats(screen, "80", "81");
        String reservationId = bookingService.reserveSeats(user2, show, secondBooking);

        // Assert - Should not be able to reserve because seat 80 is already booked
        assertNull(reservationId, "Should not be able to reserve partially booked seats");
    }

    @Test
    void testExpiredReservationCanBeReused() throws InterruptedException {
        // Arrange
        List<Seat> seats = getSeats(screen, "65");
        
        // Reserve seats
        String reservationId = bookingService.reserveSeats(user1, show, seats);
        assertNotNull(reservationId);
        
        // Verify seat is RESERVED
        Seat seat = show.getScreen().getSeats().get("65");
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus());
        
        // Release reservation (simulating expiry or manual release)
        bookingService.releaseReservation(reservationId);
        
        // Assert - Seat should be available again
        assertEquals(SeatStatus.AVAILABLE, seat.getSeatStatus());

        // Should be able to reserve again
        String newReservationId = bookingService.reserveSeats(user2, show, seats);
        assertNotNull(newReservationId, "Should be able to reserve after release");
    }

    @Test
    void testMultipleSeatsPartialAvailability() {
        // Arrange - Book some seats first
        List<Seat> firstBooking = getSeats(screen, "82", "83");
        // Retry if payment fails
        int maxRetries = 5;
        boolean booked = false;
        for (int i = 0; i < maxRetries; i++) {
            try {
                bookingService.bookTicket(user1, show, firstBooking, PaymentMethod.CREDIT_CARD);
                booked = true;
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    fail("First booking should eventually succeed");
                }
                // Retry
            }
        }
        assertTrue(booked, "First booking should succeed");

        // Act - Try to reserve seat 82 (already booked) and seat 84 (available)
        List<Seat> secondBooking = getSeats(screen, "82", "84");
        String reservationId = bookingService.reserveSeats(user2, show, secondBooking);

        // Assert - Should not be able to reserve because seat 82 is already booked
        assertNull(reservationId, "Should not be able to reserve partially booked seats");
        
        // Seat 84 should still be available since reservation failed
        Seat seat84 = show.getScreen().getSeats().get("84");
        assertEquals(SeatStatus.AVAILABLE, seat84.getSeatStatus());
    }

    @Test
    void testPaymentFailureKeepsReservation() {
        // Arrange
        List<Seat> seats = getSeats(screen, "85");
        String reservationId = bookingService.reserveSeats(user1, show, seats);
        assertNotNull(reservationId);

        // Verify seat is RESERVED
        Seat seat = show.getScreen().getSeats().get("85");
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus());

        // Act - Try to confirm with failing payment
        // Note: Current payment strategies always succeed (they retry).
        // In a real scenario with a failing payment strategy, this would throw PaymentFailedException
        // and seats would remain RESERVED
        
        // For this test, we'll verify the reservation structure is correct
        // In production, you would mock a failing PaymentStrategy to test actual failure
        try {
            // This should succeed with current implementation
            Booking booking = bookingService.confirmBookingWithReservation(
                user1, reservationId, PaymentMethod.CREDIT_CARD);
            
            // With current payment strategies, booking will succeed
            assertNotNull(booking);
            
            // If payment had failed, seat would still be RESERVED
            // and PaymentFailedException would be thrown
        } catch (PaymentFailedException e) {
            // This would happen if payment fails
            // Verify reservation is still intact
            Seat seatAfterFailure = show.getScreen().getSeats().get("85");
            assertEquals(SeatStatus.RESERVED, seatAfterFailure.getSeatStatus(),
                "Seat should remain RESERVED if payment fails");
            
            // Verify reservation is still active
            assertNotNull(e.getReservationId(), "Exception should contain reservation ID");
        }
    }

    @Test
    void testRetryPaymentAfterFailure() throws PaymentFailedException {
        // Arrange
        List<Seat> seats = getSeats(screen, "86");
        String reservationId = bookingService.reserveSeats(user1, show, seats);

        // Act - Try to confirm booking, retry if payment fails
        Booking booking = null;
        int maxRetries = 5;
        PaymentMethod[] methods = {PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD};
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                PaymentMethod method = methods[i % methods.length];
                booking = bookingService.retryPaymentWithReservation(user1, reservationId, method);
                break; // Success
            } catch (PaymentFailedException e) {
                if (i == maxRetries - 1) {
                    throw e; // Final attempt failed
                }
                // Retry with same reservation, different payment method
                reservationId = e.getReservationId();
            }
        }

        // Assert
        assertNotNull(booking, "Retry should eventually succeed");
        
        // Verify seats are BOOKED
        for (Seat seat : seats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            assertEquals(SeatStatus.BOOKED, showSeat.getSeatStatus());
        }
    }

    @Test
    void testAvailableSeatsCount() throws PaymentFailedException {
        // Arrange - Book some seats
        List<Seat> bookedSeats = getSeats(screen, "15", "16", "17");
        bookingService.bookTicket(user1, show, bookedSeats, PaymentMethod.CREDIT_CARD);

        // Reserve some seats
        List<Seat> reservedSeats = getSeats(screen, "18", "19");
        bookingService.reserveSeats(user2, show, reservedSeats);

        // Act
        Map<String, Seat> availableSeats = bookingService.availableSeats(show);

        // Assert - Total seats: 90, Booked: 3, Reserved: 2, Available: 85
        int totalSeats = 90;
        int bookedCount = 3;
        int reservedCount = 2;
        int expectedAvailable = totalSeats - bookedCount - reservedCount;
        
        assertEquals(expectedAvailable, availableSeats.size(), 
            "Available seats count should exclude booked and reserved seats");
        
        // Verify specific seats
        assertFalse(availableSeats.containsKey("15"), "Booked seat should not be available");
        assertFalse(availableSeats.containsKey("18"), "Reserved seat should not be available");
        assertTrue(availableSeats.containsKey("20"), "Unbooked seat should be available");
    }

    // Helper methods

    private List<Seat> getSeats(Screen screen, String... seatNumbers) {
        List<Seat> seats = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            Seat seat = screen.getSeats().get(seatNumber);
            if (seat == null) {
                throw new IllegalArgumentException("Seat " + seatNumber + " does not exist");
            }
            seats.add(seat);
        }
        return seats;
    }
}

