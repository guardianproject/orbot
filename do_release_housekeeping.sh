# update geoip files from tor
cd libs
./build-geoip-jar.sh
cd ..
./update_snowflake_bridges.sh