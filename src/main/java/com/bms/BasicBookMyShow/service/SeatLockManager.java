package com.bms.BasicBookMyShow.service;

import com.bms.BasicBookMyShow.model.SeatReservation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages fine-grained locking for seat operations per show
 * Uses ReadWriteLock to allow concurrent reads but exclusive writes
 */
public class SeatLockManager {
    // Lock per show for better concurrency (instead of global lock)
    private final Map<String, ReentrantReadWriteLock> showLocks = new ConcurrentHashMap<>();
    
    // Active reservations per show
    private final Map<String, SeatReservation> reservations = new ConcurrentHashMap<>();
    
    // Seat reservations by showId -> seatNumber -> reservationId
    private final Map<String, Map<String, String>> seatReservationMap = new ConcurrentHashMap<>();

    /**
     * Gets or creates a lock for a specific show
     */
    private ReentrantReadWriteLock getShowLock(String showId) {
        return showLocks.computeIfAbsent(showId, k -> new ReentrantReadWriteLock(true)); // Fair lock
    }

    /**
     * Acquires read lock for reading seat availability (allows concurrent reads)
     * Acquires the read lock if the write lock is not held by another thread and returns immediately.
     */
    public void acquireReadLock(String showId) {
        getShowLock(showId).readLock().lock();
    }

    /**
     * Releases read lock
     */
    public void releaseReadLock(String showId) {
        ReentrantReadWriteLock lock = showLocks.get(showId);
        if (lock != null) {
            lock.readLock().unlock();
        }
    }

    /**
     * Acquires write lock for modifying seats (exclusive access)
     */
    public void acquireWriteLock(String showId) {
        getShowLock(showId).writeLock().lock();
    }

    /**
     * Releases write lock
     */
    public void releaseWriteLock(String showId) {
        ReentrantReadWriteLock lock = showLocks.get(showId);
        if (lock != null) {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a reservation for seats
     */
    public SeatReservation createReservation(SeatReservation reservation) {
        String showId = reservation.getShowId();
        reservations.put(reservation.getReservationId(), reservation);
        
        seatReservationMap.computeIfAbsent(showId, k -> new ConcurrentHashMap<>())
            .putAll(reservation.getSeatNumbers().stream()
                .collect(java.util.stream.Collectors.toMap(
                    seatNum -> seatNum,
                    seatNum -> reservation.getReservationId()
                )));
        
        return reservation;
    }

    /**
     * Gets a reservation by ID
     */
    public SeatReservation getReservation(String reservationId) {
        return reservations.get(reservationId);
    }

    /**
     * Checks if seat is reserved
     */
    public boolean isSeatReserved(String showId, String seatNumber) {
        Map<String, String> seatReservations = seatReservationMap.get(showId);
        if (seatReservations == null) {
            return false;
        }
        String reservationId = seatReservations.get(seatNumber);
        if (reservationId == null) {
            return false;
        }
        SeatReservation reservation = reservations.get(reservationId);
        return reservation != null && !reservation.isExpired();
    }

    /**
     * Removes a reservation (e.g., after booking is confirmed or expired)
     */
    public void removeReservation(String reservationId) {
        SeatReservation reservation = reservations.remove(reservationId);
        if (reservation != null) {
            Map<String, String> seatReservations = seatReservationMap.get(reservation.getShowId());
            if (seatReservations != null) {
                reservation.getSeatNumbers().forEach(seatReservations::remove);
            }
        }
    }

    /**
     * Gets the seat reservation map for a show (for internal use)
     * @param showId the show ID
     * @return map of seatNumber to reservationId, or null if show has no reservations
     */
    Map<String, String> getSeatReservationMap(String showId) {
        return seatReservationMap.get(showId);
    }
}

