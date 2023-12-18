# assumes tor is in a directory next to orbot 
# make sure tor-android is up to date, particularly external/tor...
rm geoip.jar
mkdir assets
cp -a ../../tor/src/config/geoip assets/
cp -a ../../tor/src/config/geoip6 assets/
zip -o geoip.jar assets/geoip assets/geoip6
rm -rf assets
