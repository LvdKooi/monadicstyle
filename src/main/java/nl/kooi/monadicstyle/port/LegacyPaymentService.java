package nl.kooi.monadicstyle.port;

import nl.kooi.monadicstyle.model.LegacyPaymentDetails;

public interface LegacyPaymentService {

    LegacyPaymentDetails getPaymentDetails(String carId);
}
