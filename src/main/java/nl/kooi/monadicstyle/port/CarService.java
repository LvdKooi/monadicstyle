package nl.kooi.monadicstyle.port;


import nl.kooi.monadicstyle.model.Car;

public interface CarService {

    Car getCar(String carId);
}
