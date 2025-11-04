package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.model.PaymentStatus;

public class EmailNotificationObserver implements PaymentObserver{
    @Override
    public void update(Payment payment) {
        if(payment.getPaymentStatus() == PaymentStatus.SUCCESSFUL){
            // calling those 3rd party APIs => GMAIL API
            System.out.println("Email sent for payment id: "+ payment.getId());
        }
    }
}