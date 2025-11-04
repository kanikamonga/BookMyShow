package com.bms.BasicBookMyShow.model;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {

    private final String id;

    private double amount;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private LocalDateTime timeStamp;

    public Payment(double amount, PaymentMethod paymentMethod) {
        this.id = IdGenerator.generateId();
        this.amount = amount;
        this.paymentStatus = PaymentStatus.PENDING;
        this.paymentMethod = paymentMethod;
        this.timeStamp = LocalDateTime.now();
    }


    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }


}
