package nl.kooi.monadicstyle.model;


import java.math.BigDecimal;

public record PaymentData(BigDecimal periodicPayment, BigDecimal employerContribution, Periodicity periodicity) {

}
