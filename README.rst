===========================
Emporiá Vue Data Downloader
===========================

This project provides a simple client application for the `Emporiá Energy Smart Home Monitor
<https://emporiaenergy.com>`_ cloud data storage.

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

The executable jar can be run with the following options::

    Option                                Description
    ------                                -----------
    --config <String>                     configuration file; CLI parameters
                                            override configured parameters!
                                            (default: <project dir>/config.properties)
    -d, --debug                           enable debug messages.
    --disable-influx                      disable the uploading to InfluxDB
    --help                                display help text
    --history, --offset <TemporalAmount>  history to download if no prior data is
                                            available (number plus time unit; one
                                            of 's', 'm', or 'h').
                                          For example, '--history 3h' downloads the
                                            last three hours of data. (default: 3h)
    --influx-db <String>                  InfluxDB database (default: electricity)
    --influx-password <String>            InfluxDB server password
    --influx-port <Integer>               InfluxDB server port (default: 8086)
    --influx-url <String>                 InfluxDB server URL (default: http://localhost)
    --influx-user <String>                InfluxDB server username
    --logfile <String>                    log to this file (default: <project dir>/application.log)
    --password <String>                   password
    -q, --quiet                           do not print any messages to the console
                                            except for errors.
    --raw [String]                        output raw JSON readings to this file or
                                            STDOUT if none is given
    --sleep <Integer>                     number of minutes to sleep between cycles
                                            (default: 5)
    --username <String>                   username

The ``username`` and ``password`` parameters are required. All parameters can be
configured in the configuration file (see ``config.properties.sample``).

If you are planning to use `InfluxDB <https://www.influxdata.com>`_, make sure to create the
database beforehand.

Example: ``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --config config.properties``