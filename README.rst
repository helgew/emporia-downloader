===========================
Emporiá Vue Data Downloader
===========================

This project provides a simple client application for the `Emporiá Energy Smart Home Monitor <https://emporiaenergy.com>`_ cloud data storage.

Requirements
============

The project assumes deployment to a Unix platform but will work on any other platform that supports java with only minor modifications (i.e. path to the logging directory). 

All external API dependencies are managed via maven, which is needed for compilation. 

Usage
=============

Compilation
-----------

Generate the executable jar (it will be saved to the ``dist/`` directory in the project directory) using the command '``mvn package``'.

Running
-----------

The executable jar can be run with the following options::

    Option                      Description                                        
    ------                      -----------                                        
    --clientapp-id <String>     AWS client ID                                      
    --config <String>           configuration file
                                  (CLI parameters override configured parameters!)
                                  (default: <project dir>/config.properties)                    
    -d, --debug                 enable debug messages.                             
    --disable-influx            disable the uploading to InfluxDB                  
    --help                      display help text                                  
    --influx-db <String>        InfluxDB database (default: electricity)           
    --influx-password <String>  InfluxDB server password                           
    --influx-port <Integer>     InfluxDB server port (default: 8086)               
    --influx-url <String>       InfluxDB server URL (default: http://localhost)    
    --influx-user <String>      InfluxDB server username                           
    --logfile <String>          log to this file                                   
                                  (default: <project dir>/application.log)                      
    --password <String>         password                                           
    --pool-id <String>          AWS user pool ID                                   
    -q, --quiet                 do not print any messages to the console except for
                                  errors.                                          
    --raw                       output raw JSON readings to STDOUT                 
    --region <String>           AWS region
    --sleep <Integer>           number of minutes to sleep between cycles [5]      
                                  (default: 5)                                     
    --username <String>         username        

The following parameters are required: ``username``, ``password``, ``clientapp-id``, ``pool-id``, and ``region``. All parameters can be configured in the configuration file (see ``config.properties.sample``).

Example: ``java -jar dist/emporia-downloader.1.0-SNAPSHOT.jar --config config.properties``

**Note:** The connection parameters (``clientapp-id``, ``pool-id``, and ``region``) are not published in this repository. Please `contact me <mailto:helgew@grajagan.org>`_ for details.
