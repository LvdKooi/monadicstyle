package nl.kooi.monadicstyle.port;


import nl.kooi.monadicstyle.model.PaymentData;

import java.math.BigDecimal;
import java.util.List;

public interface ContributionService {

    BigDecimal getMonthlyEmployeeContribution(String carId);

    BigDecimal sumOfContributions(List<PaymentData> paymentDetails);
}
