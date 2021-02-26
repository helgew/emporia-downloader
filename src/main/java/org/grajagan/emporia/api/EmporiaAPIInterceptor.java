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

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.grajagan.aws.CognitoAuthenticationManager;

import java.io.IOException;

@Data
@Log4j2
public class EmporiaAPIInterceptor implements Interceptor {
    private final CognitoAuthenticationManager authenticationManager;

    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.newBuilder()
                .header("User-Agent", "Dart/2.5 (dart:io)")
                .header("authtoken", authenticationManager.getIdentityToken())
                .method(original.method(), original.body()).build();

        log.debug(request.toString());
        return chain.proceed(request);
    }
}
