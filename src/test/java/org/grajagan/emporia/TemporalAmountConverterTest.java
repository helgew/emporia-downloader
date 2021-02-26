package org.grajagan.emporia;

/*-
 * #%L
 * Emporia Energy API Client
 * %%
 * Copyright (C) 2002 - 2021 Helge Weissig
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import joptsimple.ValueConversionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporalAmountConverterTest {

    @Test
    void testConvert() {
        TemporalAmountConverter converter = new TemporalAmountConverter();
        TemporalAmount amount = converter.convert("h");
        assertEquals(amount, Duration.ofHours(1));
        amount = converter.convert("3m");
        assertEquals(amount, Duration.ofMinutes(3));
        amount = converter.convert("15s");
        assertEquals(amount, Duration.ofSeconds(15));

        Assertions.assertThrows(ValueConversionException.class,
                () -> { converter.convert("1x"); });
    }
}
