package com.bms.BasicBookMyShow.service;

import com.bms.BasicBookMyShow.model.*;
import com.bms.BasicBookMyShow.payment.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Improved BookingService with fine-grained locking and seat reservation support
 * for handling concurrent booking requests
 */
public class BookingService {
    private final List<Movie> movies;
    private final List<Multiplex> multiplexes;
    private final Map<String, Show> shows;
    private final Map<String, Booking> bookings;
    private final SeatLockManager seatLockManager;

    private static BookingService instance;
    private PaymentService paymentService;

    private BookingService() {
        this.movies = new ArrayList<>();
        this.multiplexes = new ArrayList<>();
        this.shows = new HashMap<>();
        this.bookings = new HashMap<>();
        this.seatLockManager = new SeatLockManager();
        this.paymentService = PaymentService.getInstance();
    }

    public Map<String, Show> getShows() {
        return shows;
    }

    public static synchronized BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    public void addMovie(Movie movie) {
        this.movies.add(movie);
    }

    public void addMultiplex(Multiplex multiplex) {
        this.multiplexes.add(multiplex);
    }

    public void addShow(Show show) {
        this.shows.put(show.getId(), show);
    }

    /**
     * Get available seats for a show (thread-safe read)
     * Automatically treats seats reserved 5+ minutes ago as available
     */
    public Map<String, Seat> availableSeats(Show show) {
        String showId = show.getId();
        seatLockManager.acquireReadLock(showId);
        try {
            Map<String, Seat> allSeats = show.getScreen().getSeats();
            Map<String, Seat> avlSeats = new HashMap<>();

            for (Map.Entry<String, Seat> entry : allSeats.entrySet()) {
                Seat seat = entry.getValue();
                if (seat.getSeatStatus() == SeatStatus.AVAILABLE) {
                    avlSeats.put(entry.getKey(), seat);
                } else if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                    // Check if reservation is expired (5+ minutes ago)
                    if (!seatLockManager.isSeatReserved(showId, seat.getSeatNumber())) {
                        // Reservation expired, seat is effectively available
                        avlSeats.put(entry.getKey(), seat);
                    }
                }
                // BOOKED seats are excluded
            }
            return avlSeats;
        } finally {
            seatLockManager.releaseReadLock(showId);
        }
    }

    public void addObserversToPaymentNotification(PaymentObserver paymentObserver) {
        paymentService.addObservers(paymentObserver);
    }

    /**
     * Reserve seats temporarily (before payment)
     * Returns reservation ID if successful, null otherwise
     */
    public String reserveSeats(User user, Show show, List<Seat> selectedSeats) {
        String showId = show.getId();
        seatLockManager.acquireWriteLock(showId);
        try {
            // Check and reserve seats atomically
            if (areSeatsAvailable(selectedSeats, show)) {
                // Mark seats as RESERVED
                markSeatsAsReserved(selectedSeats, show);

                // Create reservation
                List<String> seatNumbers = selectedSeats.stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.toList());
                
                SeatReservation reservation = new SeatReservation(
                    showId, seatNumbers, user.getId());
                
                seatLockManager.createReservation(reservation);
                return reservation.getReservationId();
            }
            return null;
        } finally {
            seatLockManager.releaseWriteLock(showId);
        }
    }

    /**
     * Complete booking after payment (using reservation)
     * This ensures seats are only booked if they were reserved by the same user
     * 
     * @param user the user making the booking
     * @param reservationId the reservation ID
     * @param paymentMethod the payment method to use
     * @return Booking object if successful, null if reservation invalid or expired
     * @throws PaymentFailedException if payment processing fails (reservation is kept intact for retry)
     */
    public Booking confirmBookingWithReservation(User user, String reservationId, PaymentMethod paymentMethod) 
            throws PaymentFailedException {
        SeatReservation reservation = seatLockManager.getReservation(reservationId);
        if (reservation == null || reservation.isExpired()) {
            return null; // Reservation expired or doesn't exist
        }

        // Verify reservation belongs to this user
        if (!reservation.belongsToUser(user.getId())) {
            return null; // Reservation doesn't belong to this user
        }

        String showId = reservation.getShowId();
        Show show = shows.get(showId);
        if (show == null) {
            return null;
        }

        seatLockManager.acquireWriteLock(showId);
        try {
            // Re-verify reservation is still valid (may have expired while waiting for lock)
            if (reservation.isExpired()) {
                return null;
            }

            // Get seats from reservation
            List<Seat> reservedSeats = reservation.getSeatNumbers().stream()
                .map(seatNum -> show.getScreen().getSeats().get(seatNum))
                .filter(seat -> seat != null && seat.getSeatStatus() == SeatStatus.RESERVED)
                .collect(Collectors.toList());

            if (reservedSeats.size() != reservation.getSeatNumbers().size()) {
                // Some seats were released or changed status
                return null;
            }

            // Calculate price before processing payment
            double price = calculatePrice(reservedSeats, show);

            // Process payment FIRST (before marking seats as BOOKED)
            PaymentStrategy paymentStrategy = PaymentStrategyFactory.getPaymentStrategy(paymentMethod);
            paymentService.setPaymentStrategy(paymentStrategy);
            Payment payment = paymentService.processServicePayment(price);

            // Check if payment was successful
            if (payment.getPaymentStatus() != PaymentStatus.SUCCESSFUL) {
                // Payment failed - keep seats as RESERVED and reservation intact for retry
                // Don't mark seats as BOOKED, don't remove reservation
                throw new PaymentFailedException(
                    "Payment processing failed. Reservation remains active for retry.",
                    reservationId,
                    payment.getId()
                );
            }

            // Payment successful - proceed with booking
            // Mark seats as BOOKED only after successful payment
            markSeatsAsBooked(reservedSeats, show);

            // Create booking
            Booking booking = new Booking(user, show, reservedSeats, price);
            booking.setPayment(payment);
            bookings.put(booking.getId(), booking);

            // Remove reservation after successful booking
            seatLockManager.removeReservation(reservationId);

            return booking;
        } finally {
            seatLockManager.releaseWriteLock(showId);
        }
    }

    /**
     * Legacy method - kept for backward compatibility
     * Now uses reservation mechanism internally
     * 
     * @throws PaymentFailedException if payment processing fails (reservation remains for retry)
     */
    public Booking bookTicket(User user, Show show, List<Seat> selectedSeats, PaymentMethod paymentMethod) 
            throws PaymentFailedException {
        // First reserve seats
        String reservationId = reserveSeats(user, show, selectedSeats);
        if (reservationId == null) {
            return null; // Seats not available
        }

        // Then confirm booking with payment
        try {
            return confirmBookingWithReservation(user, reservationId, paymentMethod);
        } catch (PaymentFailedException e) {
            // If payment fails, reservation is kept intact - user can retry
            // Optionally, you could automatically release reservation here
            // For now, we'll throw exception so caller can decide what to do
            throw e;
        }
    }

    public void confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null && booking.getBookingStatus() == BookingStatus.PENDING) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            System.out.println("Booking Confirmed: " + bookingId);
        }
    }

    public void cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null && booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            String showId = booking.getShow().getId();
            seatLockManager.acquireWriteLock(showId);
            try {
                booking.setBookingStatus(BookingStatus.CANCELLED);
                markSeatsAsAvailable(booking.getReservedSeats(), booking.getShow());
                System.out.println("Booking Cancelled: " + bookingId);
            } finally {
                seatLockManager.releaseWriteLock(showId);
            }
        }
    }

    /**
     * Retry payment for a failed reservation
     * Useful when payment fails and user wants to try again with same or different payment method
     * 
     * @param user the user making the booking
     * @param reservationId the reservation ID from previous failed attempt
     * @param paymentMethod the payment method to use (can be different from previous attempt)
     * @return Booking object if successful
     * @throws PaymentFailedException if payment fails again
     */
    public Booking retryPaymentWithReservation(User user, String reservationId, PaymentMethod paymentMethod) 
            throws PaymentFailedException {
        // Simply call confirmBookingWithReservation - it will handle expired reservations
        return confirmBookingWithReservation(user, reservationId, paymentMethod);
    }

    /**
     * Release reservation (if user abandons booking)
     */
    public void releaseReservation(String reservationId) {
        SeatReservation reservation = seatLockManager.getReservation(reservationId);
        if (reservation == null) {
            return;
        }

        String showId = reservation.getShowId();
        Show show = shows.get(showId);
        if (show == null) {
            return;
        }

        seatLockManager.acquireWriteLock(showId);
        try {
            // Release seats back to AVAILABLE
            for (String seatNumber : reservation.getSeatNumbers()) {
                Seat seat = show.getScreen().getSeats().get(seatNumber);
                if (seat != null && seat.getSeatStatus() == SeatStatus.RESERVED) {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                }
            }
            seatLockManager.removeReservation(reservationId);
        } finally {
            seatLockManager.releaseWriteLock(showId);
        }
    }

    private void markSeatsAsAvailable(List<Seat> selectedSeats, Show show) {
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            if (showSeat != null) {
                showSeat.setSeatStatus(SeatStatus.AVAILABLE);
            }
        }
    }

    private double calculatePrice(List<Seat> selectedSeats, Show show) {
        return selectedSeats.stream().mapToDouble(Seat::getCategoryPrice).sum() 
            + (show.getShowPrice() * selectedSeats.size());
    }

    private void markSeatsAsReserved(List<Seat> selectedSeats, Show show) {
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            if (showSeat != null) {
                showSeat.setSeatStatus(SeatStatus.RESERVED);
            }
        }
    }

    private void markSeatsAsBooked(List<Seat> selectedSeats, Show show) {
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            if (showSeat != null) {
                showSeat.setSeatStatus(SeatStatus.BOOKED);
            }
        }
    }

    /**
     * Checks if seats are available for booking
     * Automatically releases expired reservations (5+ minutes old) and treats them as available
     * 
     * NOTE: This method must be called while holding a write lock (from reserveSeats)
     * because it may modify seat status when releasing expired reservations
     */
    private boolean areSeatsAvailable(List<Seat> selectedSeats, Show show) {
        String showId = show.getId();
        for (Seat seat : selectedSeats) {
            Seat showSeat = show.getScreen().getSeats().get(seat.getSeatNumber());
            if (showSeat == null) {
                return false;
            }
            
            if (showSeat.getSeatStatus() == SeatStatus.AVAILABLE) {
                continue; // Seat is available
            }
            
            if (showSeat.getSeatStatus() == SeatStatus.RESERVED) {
                // Check if reservation expired (seats reserved 5+ minutes ago)
                // isSeatReserved() returns false if reservation is expired
                if (!seatLockManager.isSeatReserved(showId, seat.getSeatNumber())) {
                    // Reservation expired, automatically release the seat and make it available
                    releaseExpiredReservationForSeat(show, seat.getSeatNumber());
                    continue; // Seat is now available
                }
                // Seat is still reserved (not expired)
                return false;
            }
            
            // Seat is BOOKED, not available
            return false;
        }
        return true;
    }
    
    /**
     * Releases an expired reservation for a specific seat
     * Automatically called when checking availability if reservation is 5+ minutes old
     * This method modifies seat status, so must be called with write lock held
     */
    private void releaseExpiredReservationForSeat(Show show, String seatNumber) {
        String showId = show.getId();
        // Get the reservation ID for this seat if it exists
        Map<String, String> seatReservations = seatLockManager.getSeatReservationMap(showId);
        if (seatReservations == null) {
            return;
        }
        
        String reservationId = seatReservations.get(seatNumber);
        if (reservationId == null) {
            return;
        }
        
        SeatReservation reservation = seatLockManager.getReservation(reservationId);
        if (reservation != null && reservation.isExpired()) {
            // Release all seats in this expired reservation
            for (String reservedSeatNumber : reservation.getSeatNumbers()) {
                Seat seat = show.getScreen().getSeats().get(reservedSeatNumber);
                if (seat != null && seat.getSeatStatus() == SeatStatus.RESERVED) {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                }
            }
            seatLockManager.removeReservation(reservationId);
        }
    }

}
