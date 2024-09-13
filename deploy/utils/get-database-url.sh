#!/bin/bash

instance="$1"

if [ "$instance" == "" ]
then
  echo "Instance identifier is required"
  exit 1
fi

dbUrl="$(aws rds describe-db-instances --db-instance-identifier "$instance" | jq -r '.DBInstances[0].Endpoint.Address')"

if [ "$dbUrl" == "" ]
then
  echo "The application must be deployed after the database"
  exit 1
fi

