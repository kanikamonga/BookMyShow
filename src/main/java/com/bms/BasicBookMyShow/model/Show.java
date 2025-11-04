package com.bms.BasicBookMyShow.model;

import com.bms.BasicBookMyShow.pricing.PricingStrategy;

import java.time.LocalDateTime;

public class Show {

    private final String id;

    private Movie movie;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Screen screen;

    private PricingStrategy pricingStrategy ;

    private static final double basePrice = 100.0;

    public double getShowPrice(){
        return pricingStrategy.calculatePrice(basePrice);
    }


    public Show(Movie movie, LocalDateTime startTime, LocalDateTime endTime, Screen screen) {
        this.id = IdGenerator.generateId();
        this.movie = movie;
        this.startTime = startTime;
        this.endTime = endTime;
        this.screen = screen;

    }

    public String getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }


}



