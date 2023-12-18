# assumes tor-android is in a directory next to orbot 
# make sure tor-android is up to date, particularly external/tor...
mkdir assets
cp -a ../../tor-android/external/tor/src/config/geoip assets/
cp -a ../../tor-android/external/tor/src/config/geoip6 assets/
zip -o geoip.jar assets/geoip assets/geoip6
rm -rf assets
