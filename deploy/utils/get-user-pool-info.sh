#!/bin/bash

# Emit output in the format
# USER_POOL_ID=....
# USER_POOL_CLIENT_ID=....
# USER_POOL_CLIENT_SECRET=....
# compatible with GITHUB_OUTPUT

user_pool_name="$1"

if [ "$user_pool_name" == "" ]
then
  echo "UserPool name is required" >&2
  exit 1
fi

user_pool_id=$(aws cognito-idp list-user-pools --max-results 10 --query "UserPools[?Name==\`${user_pool_name}\`].Id | [0]" --output text)

if [ "$user_pool_id" == "" -o "$user_pool_id" == "null" -o "$user_pool_id" == "None" ]
then
  echo "Could not get the UserPool [${user_pool_name}]. Make sure to deploy Cognito UserPool before the application"  >&2
  exit 1
fi

client_id=$(aws cognito-idp list-user-pool-clients --user-pool-id $user_pool_id --query 'UserPoolClients[?ClientName==`app-token-client`].ClientId | [0]' --output text)

if [ "$client_id" == "" -o "$client_id" == "null" -o "$client_id" == "None" ]
then
  echo "Could not get the Cognito Client [app-token-client]. Is the Cognito deployment complete?"  >&2
  exit 1
fi

client_secret=$(aws cognito-idp describe-user-pool-client --user-pool-id $user_pool_id --client-id $client_id --query 'UserPoolClient.ClientSecret' --output text)

if [ "$client_secret" == "" ]
then
  echo "Could not get the Cognito Client Secret [app-token-client]. Check error messages"  >&2
  exit 1
fi

echo "USER_POOL_ID=$user_pool_id"
echo "USER_POOL_CLIENT_ID=$client_id"
echo "USER_POOL_CLIENT_SECRET=$client_secret"
