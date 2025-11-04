package com.bms.BasicBookMyShow.pricing;

public class OffPeakStrategy implements PricingStrategy{

    @Override
    public double calculatePrice(double basePrice) {
        return basePrice*0.9;
    }
}
