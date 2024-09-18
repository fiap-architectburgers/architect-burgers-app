#!/bin/bash

# Emit output in the format
# API_ID=....
# API_URL=....
# compatible with GITHUB_OUTPUT

api_name="$1"

if [ "$api_name" == "" ]
then
  echo "API name is required" >&2
  exit 1
fi

api_id=$(aws apigatewayv2 get-apis --query "Items[?Name==\`${api_name}\`].ApiId | [0]" --output text)

if [ "$api_id" == "" -o "$api_id" == "null" -o "$api_id" == "None" ]
then
  echo "Could not get the API [${api_name}]. Make sure to deploy API Gateway before the application"  >&2
  exit 1
fi

api_endpoint=$(aws apigatewayv2 get-apis --query "Items[?Name==\`${api_name}\`].ApiEndpoint | [0]" --output text)

if [ "$api_endpoint" == "" -o "$api_endpoint" == "null" -o "$api_endpoint" == "None" ]
then
  echo "Error - Could not get the API Endpoint [${api_name}]"  >&2
  exit 1
fi

# Assuming that we are working with a single stage
stage_name=$(aws apigatewayv2 get-stages --api-id $api_id --query 'Items[].StageName | [0]' --output text)

if [ "$stage_name" == "" -o "$stage_name" == "null" -o "$stage_name" == "None" ]
then
  echo "Error - Could not get the API Stage [${api_name}]. Is the Api deployed?"  >&2
  exit 1
fi

echo "API_ID=$api_id"
echo "API_URL=${api_endpoint}/${stage_name}"
