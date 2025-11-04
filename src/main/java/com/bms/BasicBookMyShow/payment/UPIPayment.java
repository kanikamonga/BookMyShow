package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.model.PaymentMethod;
import com.bms.BasicBookMyShow.model.PaymentStatus;

public class UPIPayment extends MockPaymentProcessor implements PaymentStrategy{

    @Override
    public Payment processPayment(double amount) {
        Payment payment  = new Payment(amount, PaymentMethod.UPI);
        // call UPI APIs by passing payment.
        PaymentStatus status = MockPaymentProcessor.mockPayment(payment);
        payment.setPaymentStatus(status);
        if (status == PaymentStatus.SUCCESSFUL) {
            System.out.println("Payment processed successfully.");
        } else {
            System.out.println("Payment failed, retrying...");
            status = PaymentStatus.SUCCESSFUL; // Simulate retry leading to success
            System.out.println("Payment retried and succeeded.");
        }
        return payment;
    }
}
