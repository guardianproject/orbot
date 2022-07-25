# Onboarding People to Kindness Mode

## Eligibility
First check to see if the user can be a snowflake proxy.
Requirements:
- The user can make a direct connection to Tor
- The user is not running Tor over a bridge

If yes, display **km_onboard** in the app and send nudge notifications.

If no, display **km_ineligible-config** in the app.

If no (bc of wifi network), display **km_ineligible-wifi** in the app.

<img width="1034" alt="eligibility UI" src="https://user-images.githubusercontent.com/1434597/180260691-2dbadbf4-b203-44c0-823d-5ddc47c42d5a.png">

## Notification Nudges
A. Send **notification A** after the app updates with launch of kindness mode.

B. Send **notification B** on the next day (after the app updates) at 9 pm local time.

C. Send **notification C** _only if_ the user hasn't activated it yet, but have been using a direct connection to Tor. Send 4 days after the app updated with kindness mode.

<img width="1077" alt="Screen Shot 2022-07-21 at 10 55 09 AM" src="https://user-images.githubusercontent.com/1434597/180259030-6652e5c3-82a3-4ad5-a448-da01566cf939.png">

## App Views
### Without challenges

**km_onboard** --> **km_active** --> **km_adjust-settings** --> **km_off**

<img width="1145" alt="Screen Shot 2022-07-21 at 11 07 03 AM" src="https://user-images.githubusercontent.com/1434597/180261587-d207fdc3-0889-4ce3-8b40-00a63a69f7fd.png">

### With challenges
Challenges will be rolled out 1-3 months after kindness mode is released.

**km_active-prompt --> km_active-challenge-start --> km_active-challenge-progress --> km_active-challenge-end --> km_achievement --> km_active-challenge-post-achievement**


Figma design file: https://www.figma.com/file/c22a0Vn4QjmoviIi9Oj5eB/Orbot-%5BLatest%5D?node-id=1407%3A11214
Figma prototype file: https://www.figma.com/proto/c22a0Vn4QjmoviIi9Oj5eB/Orbot-%5BLatest%5D?page-id=1407%3A11214&node-id=1418%3A17138&viewport=504%2C-1090%2C0.6&scaling=min-zoom&starting-point-node-id=1418%3A17138&show-proto-sidebar=1
