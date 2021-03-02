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

import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

@Log4j2
public class LogJsonInterceptor implements Interceptor {
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        String channel = request.url().queryParameter("channel");
        String deviceGid = request.url().queryParameter("deviceGid");

        Response response = chain.proceed(request);

        if (log.isTraceEnabled() && response.body() != null && channel != null) {
            String rawJson = response.body().string();
            String finalJson = rawJson.replaceFirst("\\{", "{\"channel\":\"" + channel + "\","
                            + "\"deviceGid\":\"" + deviceGid + "\",");
            System.out.println(finalJson);

            // Re-create the response before returning it because body can be read only once
            response = response.newBuilder()
                    .body(ResponseBody.create(response.body().contentType(), rawJson)).build();
        }

        return response;
    }
}
