package com.bms.BasicBookMyShow.model;

public enum SeatCategory {
    SILVER(150),
    GOLD(200),
    PLATINUM(250);


    private final double price;

    SeatCategory(double price){
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}
