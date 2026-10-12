package gg.crystalized.lobby.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatisticsPercentTest {

    @Test
    void partialRecordIsNotZero() {
        assertEquals("70%", Statistics.formatPercent(7, 10));
    }

    @Test
    void repeatingDecimalRounds() {
        assertEquals("33%", Statistics.formatPercent(1, 3));
    }

    @Test
    void winlessIsZero() {
        assertEquals("0%", Statistics.formatPercent(0, 10));
    }

    @Test
    void perfectRecordIsHundred() {
        assertEquals("100%", Statistics.formatPercent(10, 10));
    }
}
