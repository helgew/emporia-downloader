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

import joptsimple.ValueConverter;
import org.grajagan.emporia.model.Scale;

public class ScaleConverter  implements ValueConverter<Scale> {
    @Override
    public Scale convert(String value) {
        return new Scale(value);
    }

    @Override
    public Class<? extends Scale> valueType() {
        return Scale.class;
    }

    @Override
    public String valuePattern() {
        return "One of 's', 'm', 'h', 'd', 'w, 'M', or 'y'";
    }

}
