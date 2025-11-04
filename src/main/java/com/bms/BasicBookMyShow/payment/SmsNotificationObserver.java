package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.model.PaymentStatus;

public class SmsNotificationObserver implements PaymentObserver{

    @Override
    public void update(Payment payment) {
        if(payment.getPaymentStatus()== PaymentStatus.SUCCESSFUL){
            // call 3rd party apis
            System.out.println("Sms sent for payment id: "+payment.getId());
        }

    }
}
