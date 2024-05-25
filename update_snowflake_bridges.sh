wget https://gitlab.torproject.org/tpo/applications/tor-browser-build/-/raw/main/projects/tor-expert-bundle/pt_config.json?ref_type=heads
mv "pt_config.json?ref_type=heads" pt_config.json
rm -f orbotservice/src/main/assets/snowflake-brokers
function bridges_conf {
  local bridge_type="$1"
  jq -r ".bridges.\"$bridge_type\" | .[]" "pt_config.json" | while read -r line; do
    echo $line >> orbotservice/src/main/assets/snowflake-brokers
  done
}

bridges_conf "snowflake"
rm pt_config.json