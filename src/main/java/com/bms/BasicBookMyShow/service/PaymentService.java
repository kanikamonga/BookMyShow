package com.bms.BasicBookMyShow.service;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.payment.PaymentObserver;
import com.bms.BasicBookMyShow.payment.PaymentStrategy;

import java.util.ArrayList;
import java.util.List;

public class PaymentService {
    private static PaymentService instance;

    private PaymentStrategy paymentStrategy;

    private List<PaymentObserver> observers = new ArrayList<>();

    private PaymentService(){

    }

    public void addObservers(PaymentObserver observer){
        observers.add(observer);
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy){
        this.paymentStrategy = paymentStrategy;
    }

    public static synchronized PaymentService getInstance(){
        if(instance ==null ){
            return new PaymentService();
        }
        return instance;
    }

    public Payment processServicePayment(double amount){
        Payment payment = paymentStrategy.processPayment(amount);
        notifyUsers(payment);
        return payment;


    }

    private void notifyUsers(Payment payment){
        for(PaymentObserver observer : observers){
            observer.update(payment);
        }
    }


}
