package com.bms.BasicBookMyShow.addlFilter;

import com.bms.BasicBookMyShow.filter.Filter;
import com.bms.BasicBookMyShow.model.Seat;
import com.bms.BasicBookMyShow.model.SeatStatus;
import com.bms.BasicBookMyShow.model.Show;

import java.util.List;
import java.util.stream.Collectors;

public class PriceFilter extends DecoratorFilterManager {
    private final double maxPrice;

    public PriceFilter(double maxPrice,DecFilter decFilter) {

        super(decFilter);
        this.maxPrice = maxPrice;
    }
    @Override
    public List<Show> apply(List<Show> shows){
        return super.apply(shows).stream()
                .filter(show -> show.getScreen().getSeats().values().stream()
                        .anyMatch(seat -> seat.getSeatStatus()== SeatStatus.AVAILABLE && Seat.getCategoryPrice(seat)<=maxPrice))
                .collect(Collectors.toList());
    }
}
