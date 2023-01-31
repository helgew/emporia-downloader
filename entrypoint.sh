#! /bin/bash

CLI_ARGS=""

CFG="./config.properties"

# mandatory Emporia settings
if [[ ! -z "${POOL_ID}"  ]]; then
	echo "pool-id=$POOL_ID" > $CFG
else
	echo "MISSING ENV: POOL_ID"
	ERR=1
fi

if [[ ! -z "${CLIENTAPP_ID}"  ]]; then
	echo "clientapp-id=$CLIENTAPP_ID" >> $CFG
else
	echo "MISSING ENV: CLIENTAPP_ID"
	ERR=1
fi

if [[ ! -z "${REGION}"  ]]; then
	echo "region=$REGION" >> $CFG
else
	echo "MISSING ENV: REGION"
	ERR=1
fi

if [[ ! -z "${USERNAME}"  ]]; then
	echo "username=$USERNAME" >> $CFG
else
	echo "MISSING ENV: USERNAME"
	ERR=1
fi

if [[ ! -z "${PASSWORD}"  ]]; then
	echo "password=$PASSWORD" >> $CFG
else
	echo "MISSING ENV: PASSWORD"
	ERR=1
fi

# optional settings
if [[ ! -z "${QUIET}"  ]] && [[ "$QUIET" == "true" ]]; then
	CLI_ARGS="-q"
fi

if [[ ! -z "${DEBUG}"  ]] && [[ "$DEBUG" == "true" ]]; then
	CLI_ARGS="$CLI_ARGS -d"
fi

if [[ ! -z "${DISABLE_INFLUX}"  ]]; then
	CLI_ARGS="$CLI_ARGS --disable-influx"
fi

if [[ ! -z "${HISTORY}"  ]]; then
	CLI_ARGS="$CLI_ARGS --history $HISTORY"
fi

if [[ ! -z "${RAW}"  ]] && [[ "$RAW" == "true" ]]; then
	CLI_ARGS="$CLI_ARGS --raw"
fi

if [[ ! -z "${SLEEP}"  ]]; then
	CLI_ARGS="$CLI_ARGS --sleep $SLEEP"
fi

if [[ ! -z "${SCALE}"  ]]; then
	CLI_ARGS="$CLI_ARGS --scale $SCALE"
fi

if [[ ! -z "${INFLUX_URL}"  ]]; then
	CLI_ARGS="$CLI_ARGS --influx-url $INFLUX_URL"
fi

if [[ ! -z "${INFLUX_BUCKET}"  ]]; then
	CLI_ARGS="$CLI_ARGS --influx-bucket $INFLUX_BUCKET"
fi

if [[ ! -z "${INFLUX_MEASUREMENT}"  ]]; then
	CLI_ARGS="$CLI_ARGS --influx-measurement $INFLUX_MEASUREMENT"
fi

if [[ ! -z "${INFLUX_PORT}"  ]]; then
	CLI_ARGS="$CLI_ARGS --influx-port $INFLUX_PORT"
fi

# mandatory settings unless influx is disabled
if [[ ! -z "${INFLUX_ORG}"  ]]; then
	CLI_ARGS="$CLI_ARGS --influx-org $INFLUX_ORG"
elif [[ -z "${DISABLE_INFLUX}" ]] && [[ ! "$DISABLE_INFLUX" == "true" ]]; then
	echo "MISSING ENV: INFLUX_ORG"
	ERR=1
fi

if [[ ! -z "${INFLUX_TOKEN}"  ]]; then
	echo "influx-token=$INFLUX_TOKEN" >> $CFG
elif [[ -z "${DISABLE_INFLUX}" ]] && [[ ! "$DISABLE_INFLUX" == "true" ]]; then
	echo "MISSING ENV: INFLUX_TOKEN"
	ERR=1
fi


if [[ ! -z "${ERR}"  ]]; then
	CLI_ARGS="/dev/stderr --help"
else
	CLI_ARGS="/dev/stdout --config $CFG $CLI_ARGS"
fi

java -jar ./emporia-downloader.jar --logfile $CLI_ARGS

