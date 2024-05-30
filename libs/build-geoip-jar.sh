rm geoip.jar
mkdir assets
git clone https://gitlab.torproject.org/tpo/core/tor.git --depth=1
cp -a tor/src/config/geoip assets/
cp -a tor/src/config/geoip6 assets/
rm -rf tor
zip -o geoip.jar assets/geoip assets/geoip6
rm -rf assets
