**Smart Connect Design Spec**

Smart connect chooses default setting based on the country. If the user is in a place that blocks Tor, like China or Russia, Orbot will default to the connection method that is known to work in those locations.
Orbot knows the location because the API determines your location using your IP address.*


If the user is not in a country that blocks Tor, Orbot will use Smart Connect by default. When the user taps 'Start VPN', the app automatically tries to connect to Tor in the order of methods below:

- Direct
- If that doesn’t work, try snowflake.
- If that doesn’t work, prompt the user to solve a captcha and give them a Tor bridge (obsf 4).
- If that doesn’t work, suggest that the user finds a friend with a bridge (custom bridge).

Note: Even if a user is in a country that doesn't block Tor, they could still be connected to a wifi network that blocks Tor.

*To be precise: The circumvention service deducts a location from the IP address using a factually standard database (MaxMind GeoIP) which is pretty good but might be wrong in edge cases. I think I remember, that the service shares this information with the calling app. So, in short: yes. However, it has also an option to let the user decide where they think they are.
I do think, though, that the IP address deduction is the better option, since the user might actually have a wrong idea about where they think their network is located. E.g. when roaming with your mobile, your internet connection typically is routed back home.

Edge Case: Example: The annual Chaos Communication Conference has its very unique Internet setup: They get access to (multiple) dark fibers and connect their own big-iron equipment to it. The congress becomes its own Internet Service Provider with its own Autonomous System number and IP address ranges. These addresses are not resolved to a location correctly, as they are typically from an unused area or are reused in different locations for these kind of setups. (They can do this, because a lot of the folks attending there are actually working at big ISPs...)
