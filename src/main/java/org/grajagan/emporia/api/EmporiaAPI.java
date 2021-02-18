package org.grajagan.emporia.api;

/*-
 * #%L
 * Emporia Energy API Client
 * %%
 * Copyright (C) 2002 - 2020 Helge Weissig
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

import org.grajagan.emporia.model.Customer;
import org.grajagan.emporia.model.Readings;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.time.Instant;

public interface EmporiaAPI {
    @GET("/customers")
    Call<Customer> getCustomer(@Query("email") String email);

    @GET("/customers/{customerId}/devices?detailed=true&hierarchy=true")
    Call<Customer> getCustomer(@Path("customerId") Integer customerId);

    @GET("/AppAPI?apiMethod=getChartUsage")
    Call<Readings> getReadings(@Query("start") Instant start,
            @Query("end") Instant end,
            @Query("type") String type,
            @Query("deviceGid") Integer deviceGid,
            @Query("scale") String scale,
            @Query("energyUnit") String unit,
            @Query("channel") String channel);
}
