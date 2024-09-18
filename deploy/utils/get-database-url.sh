#!/bin/bash

# Emit output in the format
# DB_URL=...................
# compatible with GITHUB_OUTPUT

instance="$1"

if [ "$instance" == "" ]
then
  echo "Instance identifier is required" >&2
  exit 1
fi

dbUrl="$(aws rds describe-db-instances --db-instance-identifier "$instance" | jq -r '.DBInstances[0].Endpoint.Address')"

if [ "$dbUrl" == "" ]
then
  echo "Could not get database url. Make sure to deploy RDS before the application"  >&2
  exit 1
fi

echo "DB_URL=$dbUrl"
