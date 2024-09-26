package nl.kooi.monadicstyle.service;


import nl.kooi.monadicstyle.model.Car;
import nl.kooi.monadicstyle.model.LegacyPaymentDetails;
import nl.kooi.monadicstyle.port.CarService;
import nl.kooi.monadicstyle.port.LegacyPaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FakeService implements CarService, LegacyPaymentService {

    public Car getCar(String carId) {
        return new Car(null);
    }

    public LegacyPaymentDetails getPaymentDetails(String carId) {
        return new LegacyPaymentDetails(BigDecimal.TEN);
    }
}
