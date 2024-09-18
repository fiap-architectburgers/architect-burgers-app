#!/bin/bash

cluster_name="$1"

if [ "$cluster_name" == "" ]
then
  echo "Cluster name is required" >&2
  exit 1
fi

cluster_status="$(aws eks describe-cluster --name "$cluster_name" | jq -r '.cluster.status')"

if [ "$cluster_status" == "" ]
then
  echo "Could not get cluster info. Make sure to deploy EKS before the application"  >&2
  exit 1
fi

if [ "$cluster_status" != "ACTIVE" ]
then
  echo "Cluster is not ready for deployment. Current status $cluster_status"  >&2
  exit 1
fi

echo "Cluster $cluster_name is active"