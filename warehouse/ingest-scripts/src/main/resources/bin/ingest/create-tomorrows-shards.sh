#!/bin/bash

if [[ `uname` == "Darwin" ]]; then
        THIS_SCRIPT=`python -c 'import os,sys;print os.path.realpath(sys.argv[1])' $0`
else
        THIS_SCRIPT=`readlink -f $0`
fi
THIS_DIR="${THIS_SCRIPT%/*}"
cd $THIS_DIR

. ./ingest-libs.sh

declare -i numshards=$NUM_SHARDS
if [[ "$1" == "" ]]; then
  declare -i numerator=1
else
  declare -i numerator=$1
fi
if [[ "$2" == "" ]]; then
  declare -i divisor=$numerator
  numerator=1
else
  declare -i divisor=$2
fi
numshards=$((numshards * numerator / divisor))

DATE=`date -d tomorrow +%Y%m%d`

echo "Generating ${numerator}/${divisor} of ${NUM_SHARDS} = $numshards shards for $DATE"

TYPES=${BULK_INGEST_DATA_TYPES},${LIVE_INGEST_DATA_TYPES},${COMPOSITE_DATA_TYPES}

ADDJARS=$THIS_DIR/$DATAWAVE_INGEST_CORE_JAR,$THIS_DIR/$COMMON_UTIL_JAR,$THIS_DIR/$DATAWAVE_CORE_JAR

$WAREHOUSE_ACCUMULO_HOME/bin/accumulo -add $ADDJARS datawave.ingest.util.GenerateShardSplits $DATE 1 ${numshards} -addShardMarkers -addDataTypeMarkers $TYPES $USERNAME $PASSWORD ${SHARD_TABLE_NAME} $WAREHOUSE_INSTANCE_NAME $WAREHOUSE_ZOOKEEPERS

$WAREHOUSE_ACCUMULO_HOME/bin/accumulo -add $ADDJARS datawave.ingest.util.GenerateShardSplits $DATE 1 ${numshards} -addShardMarkers -addDataTypeMarkers $TYPES $USERNAME $PASSWORD ${ERROR_SHARD_TABLE_NAME} $WAREHOUSE_INSTANCE_NAME $WAREHOUSE_ZOOKEEPERS

$WAREHOUSE_ACCUMULO_HOME/bin/accumulo -add $ADDJARS datawave.ingest.util.GenerateShardSplits $DATE 1 ${numshards} -addShardMarkers $USERNAME $PASSWORD ${QUERYMETRICS_SHARD_TABLE_NAME} $WAREHOUSE_INSTANCE_NAME $WAREHOUSE_ZOOKEEPERS
