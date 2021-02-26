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

    Option                                                        Description
    ------                                                        -----------
    --config <String>                                             configuration file; CLI parameters override configured
                                                                    parameters! (default: <project dir>/config.properties)
    -d, --debug                                                   enable debug messages.
    --disable-influx                                              disable the uploading to InfluxDB
    --help                                                        display help text
    --history, --offset <A number plus time unit (one of          history to download if no prior data is available.
      's', 'm', 'h', 'd', 'w', 'M', or 'y')>                      For example, '--history 3w' downloads the last 3 weeks
                                                                    of data.
                                                                  Note that the availability of data is subject to
                                                                    Emporia's data retention policies. (default: 3h)
    --influx-db <String>                                          InfluxDB database (default: electricity)
    --influx-password <String>                                    InfluxDB server password
    --influx-port <Integer>                                       InfluxDB server port (default: 8086)
    --influx-url <String>                                         InfluxDB server URL (default: http://localhost)
    --influx-user <String>                                        InfluxDB server username
    --logfile <String>                                            log to this file (default: <project dir>/application.log)
    --password <String>                                           password
    -q, --quiet                                                   do not print any messages to the console except for
                                                                    errors.
    --raw                                                         output raw JSON readings to STDOUT
    --scale <One of 's', 'm', 'h', 'd', 'w, 'M', or 'y'>          scale of the data
                                                                  For example, '--scale d' will download a datapoint per
                                                                    day. (default: 1SEC)
    --sleep <Integer>                                             number of minutes to sleep between cycles.
                                                                  This parameter will be adjusted such that it is always
                                                                    greater than the scale.
                                                                  If a value of 0 is given, the downloader exits after
                                                                    one dataset has been downloaded. (default: 5)
    --username <String>                                           username

The ``username`` and ``password`` parameters are required. All parameters can be
configured in the configuration file (see ``config.properties.sample``).

If you are planning to use `InfluxDB <https://www.influxdata.com>`_, make sure to create the
database beforehand.

Example: ``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --config config.properties``