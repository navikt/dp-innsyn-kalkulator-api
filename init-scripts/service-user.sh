#!/usr/bin/env bash

if test -f /secrets/serviceuser/srvdp-kalkulator-api/username;
then
    export  STS_USERNAME=$(cat /secrets/serviceuser/srvdp-graphql/username)
    echo "exported STS_USERNAME"
else
   echo "SECRETS not mounted"
fi
if test -f /secrets/serviceuser/srvdp-kalkulator-api/password;
then
    export  STS_PASSWORD=$(cat /secrets/serviceuser/srvdp-graphql/password)
    echo "exported STS_PASSWORD"
fi