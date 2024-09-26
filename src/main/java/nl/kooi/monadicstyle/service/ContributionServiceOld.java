package nl.kooi.monadicstyle.service;


import lombok.RequiredArgsConstructor;
import nl.kooi.monadicstyle.model.PaymentData;
import nl.kooi.monadicstyle.model.Periodicity;
import nl.kooi.monadicstyle.port.CarService;
import nl.kooi.monadicstyle.port.ContributionService;
import nl.kooi.monadicstyle.port.LegacyPaymentService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Order(0)
@RequiredArgsConstructor
public class ContributionServiceOld implements ContributionService {
    private final CarService carRestService;
    private final LegacyPaymentService legacyPaymentService;

    @Override
    public BigDecimal getMonthlyEmployeeContribution(String carId) {

        var car = carRestService.getCar(carId);

        if (car != null &&
                car.currentDriver() != null &&
                car.currentDriver().leaseContract() != null &&
                car.currentDriver().leaseContract().paymentData() != null) {

            var paymentDetails = car.currentDriver().leaseContract().paymentData();

            if (paymentDetails.periodicPayment() != null &&
                    paymentDetails.employerContribution() != null &&
                    paymentDetails.periodicity() == Periodicity.MONTHLY) {

                return paymentDetails.periodicPayment().subtract(paymentDetails.employerContribution());
            }
        }

        var legacyPaymentDetails = legacyPaymentService.getPaymentDetails(carId);

        if (legacyPaymentDetails != null && legacyPaymentDetails.periodicPayment() != null) {
            return legacyPaymentDetails.periodicPayment();
        }

        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumOfContributions(List<PaymentData> paymentDetails) {
        var sum = BigDecimal.ZERO;

        if (paymentDetails != null) {

            for (var detail : paymentDetails) {

                if (detail != null && detail.periodicPayment() != null &&
                        detail.periodicity() == Periodicity.MONTHLY) {

                    sum = sum.add(detail.periodicPayment());
                }
            }
        }

        return sum;
    }
}


