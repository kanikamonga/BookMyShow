**Singleton**, **Factory**, **Strategy**, **Decorator**, and **Observer** design patterns in a concise and structured way:

# Design Patterns in Java

This repository contains implementations of common **Design Patterns** in Java. These patterns are used to solve common object-oriented design problems, ensuring modular, scalable, and maintainable code.

## Table of Contents

1. [Singleton Pattern](#singleton-pattern)
2. [Factory Pattern](#factory-pattern)
3. [Strategy Pattern](#strategy-pattern)
4. [Decorator Pattern](#decorator-pattern)
5. [Observer Pattern](#observer-pattern)

---

## 1. Singleton Pattern

### Overview
The **Singleton** pattern ensures that a class has only one instance and provides a global point of access to it. It is often used when exactly one instance of a class is needed to coordinate actions across the system, such as logging, database connections, or configuration settings.

### Key Features
- Ensures a single instance.
- Provides a global access point.
- Lazy initialization or eager initialization.

### Example Code

```java
public class BookingService {
    private static BookingService instance;

    private BookingService() {  
    this.movies = new ArrayList<>();  
    this.multiplexes = new ArrayList<>();  
    this.shows = new HashMap<>();  
    this.bookings = new HashMap<>();  
	}

   public static synchronized BookingService getInstance(){  
	    if(instance ==null){  
	        instance = new BookingService();  
	    }  
	    return instance;  
	}
} 

```
## 2. Factory Pattern

### Overview

The **Factory** pattern provides an interface for creating objects in a super class but allows subclasses to alter the type of objects that will be created. This pattern is useful when you have multiple classes that share a common interface and you want to instantiate them in a consistent manner.

### Key Features

-   Creates objects without exposing the instantiation logic.
-   Promotes loose coupling by abstracting object creation.
-   Can have multiple concrete factory implementations.
public class PaymentStrategyFactory {  
  
  
  
  
  ``` Java
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
```

## 3. Strategy Pattern

### Overview

The **Strategy** pattern allows selecting an algorithm at runtime. It defines a family of algorithms, encapsulates each one, and makes them interchangeable. This pattern is used to change the behavior of an object dynamically.

### Key Features

-   Decouples the algorithm from the context in which it is used.
-   Allows runtime selection of algorithms.
-   Promotes flexibility by allowing algorithms to vary independently.
## Example Code
```Java
public interface PaymentStrategy {
    void pay(int amount);
}

public class CreditCardPayment implements PaymentStrategy {
    @Override
    public void pay(int amount) {
        System.out.println("Paid " + amount + " using Credit Card");
    }
}

public class PayPalPayment implements PaymentStrategy {
    @Override
    public void pay(int amount) {
        System.out.println("Paid " + amount + " using PayPal");
    }
}

public class PaymentContext {
    private PaymentStrategy strategy;

    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void executePayment(int amount) {
        strategy.pay(amount);
    }
}
```
## Example from Source code
``` Java
public interface PaymentStrategy {  
    Payment processPayment(double amount);  
}

public class CreditCardPayment extends MockPaymentProcessor implements PaymentStrategy {  
  
  // one of the example from source code
    @Override  
  public Payment processPayment(double amount) {  
        Payment payment  = new Payment(amount, PaymentMethod.CREDIT_CARD);  
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
```
## 4. Decorator Pattern

### Overview

The **Decorator** pattern allows behavior to be added to an individual object, dynamically, without affecting the behavior of other objects from the same class. This is useful when you want to add responsibilities to objects without altering their code.

### Key Features

-   Attaches additional responsibilities to an object.
-   Provides a flexible alternative to subclassing for extending functionality.
-   Supports chaining of decorators.

### Example Code
``` Java
public interface Car {
    void assemble();
}

public class BasicCar implements Car {
    @Override
    public void assemble() {
        System.out.println("Basic Car.");
    }
}

public class CarDecorator implements Car {
    protected Car decoratedCar;

    public CarDecorator(Car car) {
        this.decoratedCar = car;
    }

    public void assemble() {
        this.decoratedCar.assemble();
    }
}

public class SportsCar extends CarDecorator {
    public SportsCar(Car car) {
        super(car);
    }

    @Override
    public void assemble() {
        super.assemble();
        System.out.println("Adding features of Sports Car.");
    }
}

public class LuxuryCar extends CarDecorator {
    public LuxuryCar(Car car) {
        super(car);
    }

    @Override
    public void assemble() {
        super.assemble();
        System.out.println("Adding features of Luxury Car.");
    }
}
```

### Example from Source Code

``` Java
package com.bms.BasicBookMyShow.addlFilter;  
  
import com.bms.BasicBookMyShow.model.Show;  
  
import java.util.List;  
  
public interface DecFilter {  
    List<Show> apply(List<Show> shows);  
}


package com.bms.BasicBookMyShow.addlFilter;  
  
  
import com.bms.BasicBookMyShow.model.Show;  
  
import java.util.List;  
  
public class DecoratorFilterManager implements DecFilter {  
    private final DecFilter decFilter;  
  
    public DecoratorFilterManager(DecFilter decFilter) {  
        this.decFilter = decFilter;  
    }  
  
    @Override  
  public List<Show> apply(List<Show> shows) {  
        return decFilter.apply(shows);  
    }  
}

package com.bms.BasicBookMyShow.addlFilter;  
  
import com.bms.BasicBookMyShow.filter.Filter;  
import com.bms.BasicBookMyShow.model.Show;  
  
import java.util.List;  
import java.util.stream.Collectors;  
  
public class GenreFilter extends DecoratorFilterManager {  
    private final String genre;  
  
    public GenreFilter(String  genre,DecFilter decFilter) {  
        super(decFilter);  
        this.genre = genre;  
    }  
  
    @Override  
  public List<Show> apply(List<Show> shows){  
        return super.apply(shows).stream()  
                .filter(show -> show.getMovie().getGenre().equals(genre))  
                .collect(Collectors.toList());  
    }  
}

```

## 5. Observer Pattern

### Overview

The **Observer** pattern defines a one-to-many dependency between objects, where a change in one object triggers updates to all dependent objects. It is particularly useful in event-driven programming, where the state of one object affects others.

### Key Features

-   Defines a one-to-many relationship between objects.
-   Subject notifies observers about state changes.
-   Observers can subscribe or unsubscribe dynamically.
## Example Code

``` Java
import java.util.ArrayList;
import java.util.List;

public interface Observer {
    void update(String message);
}

public class ConcreteObserver implements Observer {
    private String name;

    public ConcreteObserver(String name) {
        this.name = name;
    }

    @Override
    public void update(String message) {
        System.out.println(name + " received message: " + message);
    }
}

public class Subject {
    private List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }
}

```

### Example from source code

```Java
package com.bms.BasicBookMyShow.payment;  
  
import com.bms.BasicBookMyShow.model.Payment;  
  
public interface PaymentObserver {  
    void update(Payment payment);  
}

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
```

## Conclusion

Design patterns are powerful tools for solving common design problems. They make systems more flexible, modular, and easier to maintain. This repository demonstrates the practical use of the **Singleton**, **Factory**, **Strategy**, **Decorator**, and **Observer** design patterns in Java. Each pattern provides a structured solution to a different type of problem encountered during software development.
