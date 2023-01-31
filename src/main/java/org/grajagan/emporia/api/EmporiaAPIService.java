package org.grajagan.emporia.api;

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

import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.configuration.Configuration;
import org.grajagan.aws.CognitoAuthenticationManager;
import org.grajagan.emporia.JacksonObjectMapper;
import org.grajagan.emporia.model.Channel;
import org.grajagan.emporia.model.Customer;
import org.grajagan.emporia.model.Readings;
import org.grajagan.emporia.model.Scale;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Log4j2
public class EmporiaAPIService {
    public static final String API_URL = "https://api.emporiaenergy.com";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SCALE    = "scale";
    public static final String MAINTENANCE_URL = "http://s3.amazonaws.com/"
            + "com.emporiaenergy.manual.ota/maintenance/maintenance.json";

    private static final int WAIT_SECONDS_BETWEEN_REQUESTS = 30;
    private static Instant lastAccess;

    private final EmporiaAPI emporiaAPI;
    private final String username;
    private final OkHttpClient simpleClient;
    private final Scale scale;

    public EmporiaAPIService(Configuration configuration) {
        CognitoAuthenticationManager authenticationManager =
                CognitoAuthenticationManager.builder().username(configuration.getString(USERNAME))
                        .password(configuration.getString(PASSWORD)).build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .callTimeout(2, TimeUnit.MINUTES)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        simpleClient = builder.build();

        OkHttpClient client = simpleClient.newBuilder()
                .addInterceptor(new EmporiaAPIInterceptor(authenticationManager))
                .addInterceptor(new LogJsonInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(JacksonConverterFactory.create(new JacksonObjectMapper()))
                .client(client)
                .build();

        emporiaAPI = retrofit.create(EmporiaAPI.class);
        username = configuration.getString(USERNAME);
        scale = (Scale) configuration.getProperty(SCALE);
        Readings.setScale(scale);
    }

    public boolean isDownForMaintenance() {
        Request request = new Request.Builder().url(MAINTENANCE_URL).build();
        boolean downForMaintenance = false;

        try {
            Response response = simpleClient.newCall(request).execute();
            downForMaintenance = response.isSuccessful();
            if (response.body() != null) {
                response.body().close();
            }
        } catch (Exception e) {
            log.warn("Exception while checking for maintenance!", e);
        }

        return downForMaintenance;
    }

    public Customer getCustomer() {
        waitBetweenCalls();
        Call<Customer> customerCall = emporiaAPI.getCustomer(username);
        Customer customer = null;
        try {
            customer = customerCall.execute().body();
            if (customer != null) {
                customerCall = emporiaAPI.getCustomer(customer.getCustomerGid());
                customer = customerCall.execute().body();
            }
        } catch (Exception e) {
            log.error("Cannot get customer!", e);
        }

        return customer;
    }

    public Readings getReadings(Channel channel, Instant start, Instant end) {
        waitBetweenCalls();
        Call<Readings> readingsCall = emporiaAPI.getReadings(start.truncatedTo(ChronoUnit.SECONDS),
                end.truncatedTo(ChronoUnit.SECONDS),
                Readings.DEFAULT_TYPE,
                channel.getDeviceGid(),
                scale.toString(),
                Readings.DEFAULT_UNIT,
                channel.getChannelNum());
        Readings readings = null;
        try {
            readings = readingsCall.execute().body();
            if (readings != null) {
                readings.setChannel(channel);
            }
        } catch (Exception e) {
            log.error("Cannot get readings!", e);
        }
        return readings;
    }

    private void waitBetweenCalls() {
        if (lastAccess == null) {
            lastAccess = Instant.now();
            return;
        }

        long secsSinceLastRequest = Instant.now().getEpochSecond() - lastAccess.getEpochSecond();
        long secsWeNeedToWait = WAIT_SECONDS_BETWEEN_REQUESTS - secsSinceLastRequest;

        if (secsWeNeedToWait > 0) {
            log.debug("Waiting " + secsWeNeedToWait + "s between calls!");
            try {
                Thread.sleep(1000 * secsWeNeedToWait);
            } catch (InterruptedException e) {
                log.warn("Interrupt: " + e.getMessage());
            }
        }

        lastAccess = Instant.now();
    }
}
