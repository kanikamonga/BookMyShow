package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;

public interface PaymentObserver {
    void update(Payment payment);
}
