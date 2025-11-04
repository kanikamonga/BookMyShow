package com.bms.BasicBookMyShow;

import com.bms.BasicBookMyShow.addlFilter.DecFilter;
import com.bms.BasicBookMyShow.addlFilter.GenreFilter;
import com.bms.BasicBookMyShow.addlFilter.LocationFilter;
import com.bms.BasicBookMyShow.addlFilter.PriceFilter;
import com.bms.BasicBookMyShow.filter.Filter;
import com.bms.BasicBookMyShow.filter.FilterManager;
import com.bms.BasicBookMyShow.filter.MovieFilter;
import com.bms.BasicBookMyShow.model.*;
import com.bms.BasicBookMyShow.service.BookingService;
import com.bms.BasicBookMyShow.service.PaymentFailedException;
import com.bms.BasicBookMyShow.sorter.SortManager;
import com.bms.BasicBookMyShow.sorter.SortingStrategy;
import com.bms.BasicBookMyShow.sorter.TimeShowSort;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class BasicBookMyShowApplication {

	// This is hardcoded since this is designing process.

	public static void main(String[] args) throws PaymentFailedException {

		BookingService bookingService = BookingService.getInstance();

		Movie pushpa2 = new Movie("Pushpa-2","Telugu","Action","2024-12-05",3);
		Movie devara = new Movie("Devara","Telugu","Action","2024-08-05",3);
		Movie RRR = new Movie("RRR","Telugu","Action Drama","2023-08-13",3);

		Multiplex multiplex = new Multiplex("Lalitha","Peddapuram");

		Screen screen1 = new Screen("Screen1",multiplex,90);
		multiplex.addScreen(screen1);

		Show show1 = new Show(RRR,
				LocalDateTime.of(2024,12,18,19,00),
				LocalDateTime.of(2024,12,18,22,00),screen1);
		Show show2 = new Show(pushpa2,
				LocalDateTime.of(2024,12,19,19,00),
				LocalDateTime.of(2024,12,19,22,00),screen1);
		Show show3 = new Show(devara,
				LocalDateTime.of(2024,12,20,19,00),
				LocalDateTime.of(2024,12,20,22,00),screen1);

		bookingService.addShow(show1);
		bookingService.addShow(show2);
		bookingService.addShow(show3);
		User user1 = new User("Yashwanth","abcd@gmail.com");


		Seat seat1 = new Seat(show1.getScreen().getSeats().get("1").getSeatNumber(),SeatCategory.GOLD);
		Seat seat2 = new Seat(show1.getScreen().getSeats().get("1").getSeatNumber(),SeatCategory.GOLD);

		Booking booking = bookingService.bookTicket(user1,show1, List.of(seat1,seat2),PaymentMethod.CREDIT_CARD);


		boolean isPaymentSuccess = true;
		if(booking!=null){
			System.out.println("Booking Successful BookingId: "+ booking.getId());
			if(isPaymentSuccess){
				bookingService.confirmBooking(booking.getId());
			}else{
				bookingService.cancelBooking(booking.getId());
			}
		}

		// default filter options
		// strategy design pattern
		Filter movieFilter = new MovieFilter("Pushpa-2");
		FilterManager filterManager = new FilterManager();
		filterManager.setFilter(movieFilter);
		List<Show> filteredShows = filterManager.applyFilter(new ArrayList<>(bookingService.getShows().values()));

		for(Show filteredShow: filteredShows){
			System.out.println(filteredShow.getMovie().getTitle());
		}


		// multiple filtering options
		//decorator design pattern
		DecFilter locationFilter = new LocationFilter("Peddapuram");
		DecFilter priceFilter = new PriceFilter(200,locationFilter);
		DecFilter genreFilter = new GenreFilter("Action",priceFilter);

		List<Show> multiFilteredShows = genreFilter.apply(new ArrayList<>(bookingService.getShows().values()));
		for(Show multiFilteredShow : multiFilteredShows){
			System.out.println(multiFilteredShow.getMovie().getTitle());
		}

		SortingStrategy sortingStrategy = new TimeShowSort();
		SortManager sortManager = new SortManager(sortingStrategy);
		List<Show> sortedShows = sortManager.getSortedShows(multiFilteredShows);
//		List<Show> sortedShows = sortManager.getSortedShows(new ArrayList<>(bookingService.getShows().values()));

		for(Show sortedShow : sortedShows){
			System.out.println(sortedShow.getMovie().getTitle());
		}

	}
}
