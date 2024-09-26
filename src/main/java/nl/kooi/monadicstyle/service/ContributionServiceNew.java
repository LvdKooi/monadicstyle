package nl.kooi.monadicstyle.service;

import lombok.RequiredArgsConstructor;
import nl.kooi.monadicstyle.model.*;
import nl.kooi.monadicstyle.port.CarService;
import nl.kooi.monadicstyle.port.ContributionService;
import nl.kooi.monadicstyle.port.LegacyPaymentService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@Order(1)
@RequiredArgsConstructor
public class ContributionServiceNew implements ContributionService {
    private final CarService carRestService;
    private final LegacyPaymentService legacyPaymentService;

    @Override
    public BigDecimal sumOfContributions(List<PaymentData> paymentDetails) {
        return Optional.ofNullable(paymentDetails)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(paysPeriodically().and(paysMonthly()))
                .map(PaymentData::periodicPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getMonthlyEmployeeContribution(String carId) {
        return findPaymentDetails(carId)
                .flatMap(this::calculateEmployeeContribution)
                .orElseGet(() -> findLegacyPayment(carId).orElse(BigDecimal.ZERO));
    }

    private Optional<BigDecimal> calculateEmployeeContribution(PaymentData paymentData) {
        return Optional.ofNullable(paymentData)
                .filter(paysPeriodically().and(employerContributes()).and(paysMonthly()))
                .map(subtract(PaymentData::periodicPayment, PaymentData::employerContribution));
    }

    private Optional<PaymentData> findPaymentDetails(String carId) {
        return Optional.ofNullable(carId)
                .map(carRestService::getCar)
                .map(Car::currentDriver)
                .map(Driver::leaseContract)
                .map(LeaseContract::paymentData);
    }

    private static Predicate<PaymentData> paysPeriodically() {
        return paymentData -> Optional.ofNullable(paymentData)
                .map(PaymentData::periodicPayment)
                .isPresent();
    }

    private static Predicate<PaymentData> employerContributes() {
        return paymentData -> Optional.ofNullable(paymentData)
                .map(PaymentData::employerContribution)
                .isPresent();
    }

    private static Predicate<PaymentData> paysMonthly() {
        return paymentData -> Optional.ofNullable(paymentData)
                .map(PaymentData::periodicity)
                .map(Periodicity.MONTHLY::equals)
                .orElse(false);
    }

    private Optional<BigDecimal> findLegacyPayment(String carId) {
        return Optional.ofNullable(carId)
                .map(legacyPaymentService::getPaymentDetails)
                .map(LegacyPaymentDetails::periodicPayment);
    }

    private static Function<PaymentData, BigDecimal> subtract(Function<PaymentData, BigDecimal> subtractFrom,
                                                              Function<PaymentData, BigDecimal> secondAmount) {
        return pd -> getAmountOrZero(pd, subtractFrom)
                .subtract(getAmountOrZero(pd, secondAmount));
    }

    private static BigDecimal getAmountOrZero(PaymentData pd, Function<PaymentData, BigDecimal> amountGetter) {
        return Optional.ofNullable(amountGetter.apply(pd)).orElse(BigDecimal.ZERO);
    }

}
