package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;

public interface PaymentStrategy {
    Payment processPayment(double amount);
}
