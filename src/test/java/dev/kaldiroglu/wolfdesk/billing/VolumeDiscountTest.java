package dev.kaldiroglu.wolfdesk.billing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolumeDiscountTest {

    @ParameterizedTest(name = "{0} tickets -> {1}%")
    @CsvSource({
            "0, 0", "500, 0",
            "501, 10", "750, 10",
            "751, 20", "1000, 20",
            "1001, 30", "5000, 30"
    })
    void discountsStartAboveEachThreshold(int ticketsOpened, int expectedPercent) {
        assertEquals(expectedPercent, VolumeDiscount.percentFor(ticketsOpened));
    }
}
