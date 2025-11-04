package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.PaymentMethod;

public class PaymentStrategyFactory {




    public static PaymentStrategy getPaymentStrategy(PaymentMethod paymentMethod){
        return switch (paymentMethod) {
            case CREDIT_CARD -> new CreditCardPayment();
            case PAYPAL -> new PaypalPayment();
            case DEBIT_CARD -> new DebitCardPayment();
            case UPI -> new UPIPayment();
            case NET_BANKING -> new NetBankingPayment();
            default -> throw new IllegalStateException("Invalid Payment method");
        };
    }
}
