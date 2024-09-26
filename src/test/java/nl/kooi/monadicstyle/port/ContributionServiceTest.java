package nl.kooi.monadicstyle.port;

import nl.kooi.monadicstyle.model.*;
import nl.kooi.monadicstyle.service.ContributionServiceNew;
import nl.kooi.monadicstyle.service.ContributionServiceOld;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringJUnitConfig({ContributionServiceOld.class, ContributionServiceNew.class})
class ContributionServiceTest {

    @Autowired
    private List<ContributionService> contributionServices;

    @MockBean
    private CarService carRestService;

    @MockBean
    private LegacyPaymentService legacyPaymentService;

    @Nested
    @DisplayName("Tests for sumOfContributions")
    class SumOfContributionsTests {

        @Test
        void shouldAddUpWhenListContainsValuesThatMatchConditions() {
            obtainAndAssertResult(getPaymentDataCombinationsThatMatchPrecoditions(), BigDecimal.valueOf(12));
        }

        @Test
        void shouldReturnZeroWhenListIsNull() {
            obtainAndAssertResult(null, BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenListContainsOnlyItemsThatDontMatchCondition() {
            obtainAndAssertResult(getPaymentDataCombinationsThatDontMatchPrecoditions(), BigDecimal.ZERO);
        }

        private void obtainAndAssertResult(Stream<PaymentData> paymentDataStream, BigDecimal expectedOutcome) {
            var input = Optional.ofNullable(paymentDataStream)
                    .map(Stream::toList)
                    .orElse(null);

            var result = contributionServices.stream()
                    .map(service -> service.sumOfContributions(input))
                    .toList();

            assertThat(result)
                    .isNotNull()
                    .allMatch(expectedOutcome::equals);
        }

        private static Stream<PaymentData> getPaymentDataCombinationsThatMatchPrecoditions() {
            return Stream.of(
                    new PaymentData(BigDecimal.TWO, BigDecimal.ONE, Periodicity.MONTHLY),
                    new PaymentData(BigDecimal.TEN, BigDecimal.TEN, Periodicity.MONTHLY),
                    new PaymentData(BigDecimal.ONE, BigDecimal.TEN, Periodicity.ANNUALLY)
            );
        }

        private static Stream<PaymentData> getPaymentDataCombinationsThatDontMatchPrecoditions() {
            return Stream.of(
                    new PaymentData(null, BigDecimal.ONE, Periodicity.MONTHLY),
                    new PaymentData(BigDecimal.TWO, BigDecimal.TEN, Periodicity.ANNUALLY),
                    null
            );
        }
    }

    @Nested
    @DisplayName("Tests for getMonthlyEmployeeContribution")
    class GetMonthlyEmployeeContributionTests {
        @Test
        void shouldCalculateContributionWhenHasMonthlyPaymentDetails() {
            when(carRestService.getCar(anyString()))
                    .thenReturn(getCarWithPaymentPeriodicity(Periodicity.MONTHLY));

            obtainAndAssertResult()
                    .allSatisfy(amount -> assertThat(amount).isNotNull().isEqualTo(BigDecimal.valueOf(9)));

            verify(legacyPaymentService, never()).getPaymentDetails(anyString());
        }

        @ParameterizedTest
        @MethodSource("getPaymentDataCombinationsThatDontMatchPrecoditions")
        void shouldRetrieveLegacyPaymentWhenHasNoMonthlyPaymentDetails(PaymentData paymentData) {
            when(carRestService.getCar(anyString()))
                    .thenReturn(getCarWithPaymentData(paymentData));

            when(legacyPaymentService.getPaymentDetails(anyString()))
                    .thenReturn(new LegacyPaymentDetails(BigDecimal.TWO));

            obtainAndAssertResult()
                    .allSatisfy(amount -> assertThat(amount).isNotNull().isEqualTo(BigDecimal.TWO));

            verify(legacyPaymentService, atLeastOnce())
                    .getPaymentDetails(anyString());
        }

        @ParameterizedTest
        @MethodSource("getLegacyPaymentDetailsThatDontMatchPrecoditions")
        void shouldDefaultToZeroWhenNoPaymentDataAndNoLegacyDetails(LegacyPaymentDetails paymentDetails) {
            when(carRestService.getCar(anyString()))
                    .thenReturn(null);

            when(legacyPaymentService.getPaymentDetails(anyString()))
                    .thenReturn(paymentDetails);

            obtainAndAssertResult()
                    .allSatisfy(amount -> assertThat(amount).isNotNull().isEqualTo(BigDecimal.ZERO));

            verify(legacyPaymentService, atLeastOnce())
                    .getPaymentDetails(anyString());
        }

        private ListAssert<BigDecimal> obtainAndAssertResult() {
            var results = contributionServices.stream()
                    .map(contributionService -> contributionService.getMonthlyEmployeeContribution("1"))
                    .toList();

            return AssertionsForInterfaceTypes.assertThat(results)
                    .isNotNull()
                    .hasSize(2);
        }

        private static Stream<PaymentData> getPaymentDataCombinationsThatDontMatchPrecoditions() {
            return Stream.of(
                    new PaymentData(null, BigDecimal.ONE, Periodicity.MONTHLY),
                    new PaymentData(BigDecimal.ONE, null, Periodicity.MONTHLY),
                    new PaymentData(BigDecimal.ONE, BigDecimal.TEN, null),
                    new PaymentData(null, null, null),
                    new PaymentData(BigDecimal.TWO, BigDecimal.TEN, Periodicity.ANNUALLY),
                    new PaymentData(BigDecimal.TWO, BigDecimal.TEN, Periodicity.SEMI_ANNUALLY),
                    new PaymentData(BigDecimal.TWO, BigDecimal.TEN, Periodicity.QUARTERLY)
            );
        }

        private static Stream<LegacyPaymentDetails> getLegacyPaymentDetailsThatDontMatchPrecoditions() {
            return Stream.of(
                    new LegacyPaymentDetails(null),
                    null
            );
        }

        private static Car getCarWithPaymentPeriodicity(Periodicity periodicity) {
            return new Car(new Driver(new LeaseContract(new PaymentData(BigDecimal.TEN, BigDecimal.ONE, periodicity))));
        }

        private static Car getCarWithPaymentData(PaymentData paymentData) {
            return new Car(new Driver(new LeaseContract(paymentData)));
        }
    }
}