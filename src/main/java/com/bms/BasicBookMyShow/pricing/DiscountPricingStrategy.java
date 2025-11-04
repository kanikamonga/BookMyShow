package com.bms.BasicBookMyShow.pricing;

public class DiscountPricingStrategy implements PricingStrategy{
    @Override
    public double calculatePrice(double basePrice) {
        return basePrice*0.5;
    }
}
