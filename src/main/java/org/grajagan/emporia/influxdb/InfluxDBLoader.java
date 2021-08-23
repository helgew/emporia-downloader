package org.grajagan.emporia.influxdb;

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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Organization;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.grajagan.emporia.model.Channel;
import org.grajagan.emporia.model.Readings;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

@Data
@RequiredArgsConstructor
@Log4j2
public class InfluxDBLoader {

    private final URL influxDbUrl;

    private final String influxDbOrg;
    private final String influxDbBucket;
    private final String influxDbToken;
    private final String measurementName;

    private InfluxDBClient influxDBclient;

    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private boolean isConnected = false;

    private int writesCount = 0;

    private void connect() {
        influxDBclient = InfluxDBClientFactory
                .create(influxDbUrl.toString(), influxDbToken.toCharArray(), influxDbOrg,
                        influxDbBucket);

        if (!dbExists()) {
            log.warn("InfluxDB bucket " + influxDbBucket + " does not exist. Creating!");
            String orgId = null;
            for (Organization o : influxDBclient.getOrganizationsApi().findOrganizations()) {
                log.debug(o.toString());
                if (o.getName().equals(influxDbOrg)) {
                    orgId = o.getId();
                }
            }

            if (orgId == null) {
                String msg = "Cannot access organization " + influxDbOrg;
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }

            influxDBclient.getBucketsApi().createBucket(influxDbBucket, orgId);
        }

        isConnected = true;
    }

    private boolean dbExists() {
        for (Bucket bucket : influxDBclient.getBucketsApi().findBuckets()) {
            String name = bucket.getName();
            log.trace(name);
            if (influxDbBucket.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Readings load(Channel channel) {
        if (!isConnected) {
            connect();
        }
        String deviceName = getNameForChannel(channel);
        String flux = "from(bucket: \"" + influxDbBucket + "\")\n" + "  |> range(start: -10y)\n"
                + "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + deviceName + "\")\n"
                + "  |> filter(fn: (r) => r[\"_field\"] == \"watts\")\n" + "  |> last()";
        QueryApi queryApi = influxDBclient.getQueryApi();

        Readings readings = new Readings();
        readings.setChannel(channel);

        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable t : tables) {
            List<FluxRecord> records = t.getRecords();
            for (FluxRecord r : records) {
                readings.setStart(r.getTime());
                readings.setEnd(r.getTime());
                readings.getUsageList().add(((Double) r.getValueByKey("_value")).floatValue());
            }
        }

        return readings;
    }

    public void save(Readings readings) {
        if (!isConnected) {
            connect();
        }

        SortedMap<Instant, Float> data = readings.getDataPoints();
        Channel channel = readings.getChannel();
        String deviceName = getNameForChannel(channel);

        // apparently, the math is done on the server!
        float multiplier = 1f; // readings.getChannel().getChannelMultiplier();

        List<Point> points = new ArrayList<>();

        for (Instant i : data.keySet()) {
            if (data.get(i) == null) {
                continue;
            }

            Point point;
            if (measurementName == null) {
                point = Point.measurement(deviceName);
            } else {
                point = Point.measurement(measurementName)
                        .addTag("DeviceGID", channel.getDeviceGid().toString())
                        .addTag("Device", channel.getChannelNum());
            }

            if (channel.getName() != null && !channel.getName().equals("")) {
                point.addTag("Device Name", channel.getName());
            }

            point.time(i.toEpochMilli(), WritePrecision.MS)
                    .addField("watts", (int) (data.get(i) * multiplier * 100) / 100.0);

            log.debug("Created point: " + point.toLineProtocol());
            points.add(point);
        }

        influxDBclient.getWriteApi().writePoints(points);
        influxDBclient.getWriteApi().flush();
        writesCount += points.size();
    }

    private String getNameForChannel(Channel channel) {
        return channel.getDeviceGid() + "-" + channel.getChannelNum();
    }
}
