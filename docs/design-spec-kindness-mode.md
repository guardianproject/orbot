# Onboarding People to Kindness Mode

## Eligibility
First check to see if the user can be a snowflake proxy.
Requirements:
- The user can make a direct connection to Tor
- The user is not running Tor over a bridge

If yes, display **km_onboard** in the app and send nudge notifications.

If no, display **km_ineligible-config** in the app.

If no (bc of wifi network), display **km_ineligible-wifi** in the app.


## Notification Nudges
A. Send **notification A** when ...

B. Send **notification B** when ...

C. Send **notification C** when ...


## App Views
### Without challenges

**km_onboard** --> **km_active** --> **km_adjust-settings** --> **km_off**

### With challenges

**km_active-prompt --> km_active-challenge-start --> km_active-challenge-progress --> km_active-challenge-end --> km_achievement --> km_active-challenge-post-achievement**
