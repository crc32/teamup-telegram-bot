# teamup-telegram-bot [![Build Status](https://travis-ci.org/dit-calendar/teamup-telegram-bot.svg?branch=master)](https://travis-ci.org/dit-calendar/teamup-telegram-bot)

This is a tool to allow user-event assignment via telegram for a [teamup](https://www.teamup.com/) calendar. It designed to help (public/collective/participatory/collaborative) groups with self-administration of work.

after you created an event in your teamup calendar

<img src="doc/img/teamup-calendar.png" height="400"/>
you can post it in your telegram group/channel, so a person can assign hirself
<img src="doc/img/telegram-bot.gif" alt="telegram-gif"/>


## how to use
To use this application in your telegram group, you must first complete following steps:
1. create a calendar on [teamup](https://www.teamup.com/)
2. create a telegram Bot
   * start a conversation with [@Botfather](https://t.me/botfather) and write `/newbot`
   * give your Bot a name, maybe a nice picture and **please mention this website in your Bot description**
   * invite the new Bot in your telegram group
4. start the program for your telegram Bot by clicking on
    [![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/dit-calendar/teamup-telegram-bot/tree/master) _(you need an account but it's free)_
   * it will build&start the program from the current source code
   * **it will not be updated automatically!**
     * if you are interessted in more features, you should check out [new releases](https://github.com/dit-calendar/teamup-telegram-bot/releases) from time to time
     * to update it: delete the program in heroku and click on the heroku button again

**This application is still in beta** and will be further developed after some [feedback](https://github.com/dit-calendar/teamup-telegram-bot/issues) from you.

_As alternative to teamup you can also use [dit-calendar](https://github.com/dit-calendar/dit-calendar.github.io)_

# for developers

start the DB with `docker-compose up`

## manual deployment
* `gradle build`
* `heroku deploy:jar build/libs/teamup-telegram-bot*-all.jar --app teamup-telegram-bot`

## manual test
* https://core.telegram.org/bots/webhooks
* check bot status `https://api.telegram.org/bot{token}/getWebhookInfo`
* send message manually
 `curl -v -k -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
 "update_id":10000,
 "message":{
   "date":1441645532,
   "chat":{
      "last_name":"Test Lastname",
      "id":1111111,
      "first_name":"Test",
      "username":"Test"
   },
   "message_id":1365,
   "from":{
      "last_name":"Test Lastname",
      "id":1111111,
      "first_name":"Test",
      "username":"Test"
   },
   "text":"/start"
 }
 }' "localhost:8443/"`
