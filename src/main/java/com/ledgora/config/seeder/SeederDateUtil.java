package com.ledgora.config.seeder;

import java.time.LocalDate;

/**
 * Shared date utility for CBS seeders. Centralises the weekday-business-date rule so every seeder
 * module uses the same logic and avoids copy-paste duplication.
 */
final class SeederDateUtil {

    private SeederDateUtil() {}

    /**
     * Returns a guaranteed weekday date (Mon-Fri). If today is Saturday or Sunday, returns the next
     * Monday. CBS operations require a working-day business date; seeding on a weekend must not
     * produce a weekend business date.
     */
    static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }
}
