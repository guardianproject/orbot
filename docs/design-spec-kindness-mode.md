# Onboarding People to Kindness Mode

## Eligibility
First check to see if the user can be a snowflake proxy.
Requirements:
- The user can make a direct connection to Tor
- The user is not running Tor over a bridge

If yes, display **km_onboard** in the app and send nudge notifications.

If no, display **km_ineligible-config** in the app.

If no (bc of wifi network), display **km_ineligible-wifi** in the app.

<img width="367" alt="Screen Shot 2022-07-21 at 10 38 11 AM" src="https://user-images.githubusercontent.com/1434597/180258735-f096d7ea-0f3e-4d50-a8b6-5d850617e63f.png">


## Notification Nudges
A. Send **notification A** after the app updates with launch of kindness mode.

B. Send **notification B** on the next day (after the app updates) at 9 pm local time.

C. Send **notification C** _only if_ the user hasn't activated it yet, but have been using a direct connection to Tor. Send 4 days after the app updated with kindness mode.

<img width="1077" alt="Screen Shot 2022-07-21 at 10 55 09 AM" src="https://user-images.githubusercontent.com/1434597/180259030-6652e5c3-82a3-4ad5-a448-da01566cf939.png">

## App Views
### Without challenges

**km_onboard** --> **km_active** --> **km_adjust-settings** --> **km_off**

<img width="1149" alt="Screen Shot 2022-07-21 at 11 01 35 AM" src="https://user-images.githubusercontent.com/1434597/180260401-631e1498-a1d7-40a6-b2e5-e79b2753ba5c.png">


### With challenges
Challenges will be rolled out 1-3 months after kindness mode is released.

**km_active-prompt --> km_active-challenge-start --> km_active-challenge-progress --> km_active-challenge-end --> km_achievement --> km_active-challenge-post-achievement**
