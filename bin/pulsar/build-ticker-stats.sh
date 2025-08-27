#!/bin/bash


# Relative path to the source directory containing the files to be zipped
source_dir="deployments/docker/infrastructure/pulsar/functions/coinbase-ticker-stats"

# Relative path to the destination directory where the zip file will be stored
destination_dir="lib"

# Name of the zip file (you can change this as needed)
zip_file="coinbase-ticker-stats.zip"

# Change to the parent directory of the source directory
cd "$(dirname "$source_dir")" || exit

# Zip up the source directory and its contents
zip -r "$(pwd)/$destination_dir/$zip_file" "$(basename "$source_dir")"

