===========================
Emporiá Vue Data Downloader
===========================

This project provides a client application for the `Emporiá Energy Smart Home Monitor
<https://emporiaenergy.com>`_ cloud data storage. It allows users to save their electricity
usage to an `InfluxDB <https://www.influxdata.com>`_ database and/or in JSON format. By default,
the downloader will run continuously and download datasets periodically. However, it can also be
used to just download a single dataset.

Requirements
============

The project assumes deployment to a Unix platform but will work on any other platform that
supports java with only minor modifications (i.e. path to the logging directory).

All external API dependencies are managed via maven, which is needed for compilation. 

Usage
=============

Compilation
-----------

Generate the executable jar (it will be saved to the ``dist/`` directory in the project
directory) using the command '``mvn package``'.

Running
-----------

The executable jar can be run with the following options (see also ``config.properties.example``)::

    Option                                            Description
    ------                                            -----------
    --config <String>                                 configuration file; CLI parameters override
                                                        configured parameters! (default:
                                                        <project dir>/config.properties)
    -d, --debug                                       enable debug messages.
    --disable-influx                                  disable the uploading to InfluxDB
    --help                                            display help text
    --history, --offset <A number plus time unit      history to download if no prior data is
      (one of 's', 'm', 'h', 'd', 'w', 'M', or 'y')>    available.
                                                      For example, '--history 3w' downloads the last 3
                                                        weeks of data.
                                                      Note that the availability of data is subject to
                                                        Emporia's data retention policies. (default:
                                                        3h)
    --influx-db <String>                              InfluxDB database (default: electricity)
    --influx-password <String>                        InfluxDB server password
    --influx-port <Integer>                           InfluxDB server port (default: 8086)
    --influx-url <String>                             InfluxDB server URL (default: http://localhost)
    --influx-user <String>                            InfluxDB server username
    --logfile <String>                                log to this file (default:
                                                        <project dir>/application.log)
    --password <String>                               password
    -q, --quiet                                       do not print any messages to the console except
                                                        for errors.
    --raw                                             output raw JSON readings to STDOUT
    --scale <One of 's', 'm', 'h', 'd', 'w, 'M', or   scale of the data
      'y'>                                            For example, '--scale d' will download a
                                                        datapoint per day. (default: s)
    --sleep <Integer>                                 number of minutes to sleep between cycles.
                                                      This parameter will be adjusted such that it is
                                                        always greater than the scale.
                                                      If a value of 0 is given, the downloader exits
                                                        after one dataset has been downloaded.
                                                        (default: 5)
    --username <String>                               username

The ``username`` and ``password`` parameters are required. All parameters can be
configured in the configuration file (see ``config.properties.sample``). Command line options take
precedent over parameters configured in the configuration file.

Electricity Usage Units
=============

All data displayed and saved to InfluxDB will be in Kilowatt-hours with the exception of
per-second data saved to InfluxDB, which will be in Watts for historical reasons.

Example Use Cases
=============

The following use cases assume that additional parameters (e.g. ``username``, ``password``, and
InfluxDB-related settings) are configured in ``config.properties``. If you are planning to use
InfluxDB, make sure to create the database beforehand.

Continuously download per-second datapoints starting 3 hours ago, saving data to InfluxDB
-----------

``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --config config.properties``

This assumes that InfluxDB specific parameters are configured in ``config.properties`` and that
all other parameters are left as defaults.

Continuously download hourly datapoints starting yesterday, saving data to InfluxDB
-----------

``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --scale h --history 1d``

In this case, the downloader will download and save the historical data and then go into a
continuous loop where it will sleep for an hour and then download new data. All data saved to
InfluxDB will be in Kilowatt-hours.

Print the last hour of per-second data to STDOUT only and quit
-----------

``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --history 1h --raw --sleep 0``

The downloader will download and print in JSON format the per-second usage for
each device going back by an hour. The data shown will be in Kilowatt-hours but any data saved to
InfluxDB will be in Watts. There will be two lines per device and channel combination as the
downloader splits the download into chunks with no more than 2000 datapoints.

