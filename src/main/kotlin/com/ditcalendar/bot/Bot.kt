package com.ditcalendar.bot

import com.ditcalendar.bot.config.*
import com.ditcalendar.bot.domain.dao.TelegramLinksTable
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.service.CalendarService
import com.ditcalendar.bot.service.assingAnnonCallbackCommand
import com.ditcalendar.bot.service.assingWithNameCallbackCommand
import com.ditcalendar.bot.telegram.service.checkGlobalStateBeforeHandling
import com.ditcalendar.bot.teamup.endpoint.CalendarEndpoint
import com.ditcalendar.bot.teamup.endpoint.EventEndpoint
import com.ditcalendar.bot.service.CommandExecution
import com.ditcalendar.bot.telegram.service.callbackResponse
import com.ditcalendar.bot.telegram.service.messageResponse
import com.elbekD.bot.Bot
import com.elbekD.bot.server
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

val helpMessage =
        """
            Commands which the bot accept
            /postcalendar {Subcalendar name} {start date as yyyy-MM-dd} {optional end date as yyyy-MM-dd} = Post subcalendar in channel
            /help = show all bot commands
        """.trimIndent()

fun main(args: Array<String>) {

    val config by config()

    val token = config[telegram_token]
    val herokuApp = config[heroku_app_name]
    val commandExecution = CommandExecution(CalendarService(CalendarEndpoint(), EventEndpoint()))
    val databaseUrl = config[database_url]

    fun createDB() {
        val dbUri = URI(databaseUrl)
        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        var dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path
        if(herokuApp.isNotBlank()) //custom config logic needed because of config lib
            dbUrl += "?sslmode=require"

        Database.connect(dbUrl, driver = "org.postgresql.Driver",
                user = username, password = password)

        transaction {
            SchemaUtils.create(TelegramLinksTable)
        }
    }

    createDB()

    val bot = if (config[webhook_is_enabled]) {
        Bot.createWebhook(config[bot_name], token) {
            url = "https://$herokuApp.herokuapp.com/$token"

            /*
            Jetty server is used to listen to incoming request from Telegram servers.
            */
            server {
                host = "0.0.0.0"
                port = config[server_port]
            }
        }
    } else Bot.createPolling(config[bot_name], token)

    bot.onCallbackQuery { callbackQuery ->
        checkGlobalStateBeforeHandling(callbackQuery.id) {
            val request = callbackQuery.data
            val originallyMessage = callbackQuery.message

            if (request == null || originallyMessage == null) {
                bot.answerCallbackQuery(callbackQuery.id, "fehlerhafte Anfrage")
            } else {
                val msgUser = callbackQuery.from
                val response = commandExecution.executeCallback(originallyMessage.chat.id.toInt(), msgUser.id, msgUser.first_name, request)

                bot.callbackResponse(response, callbackQuery, originallyMessage)
            }
        }
    }

    //for deeplinking
    bot.onCommand("/start") { msg, opts ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {

            bot.deleteMessage(msg.chat.id, msg.message_id)
            val msgUser = msg.from
            //if message user is not set, we can't process
            if (msgUser == null) {
                bot.sendMessage(msg.chat.id, "fehlerhafte Anfrage")
            } else {
                if (opts != null && opts.startsWith("assign")) {

                    val taskId: String = opts.substringAfter("assign_")
                    if (taskId.isNotBlank()) {
                        val assignMeButton = InlineKeyboardButton("Mit Telegram Namen", callback_data = assingWithNameCallbackCommand + taskId)
                        val annonAssignMeButton = InlineKeyboardButton("Annonym", callback_data = assingAnnonCallbackCommand + taskId)
                        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(assignMeButton, annonAssignMeButton)))
                        bot.sendMessage(msg.chat.id, "Darf ich dein Namen verwenden?", "MarkdownV2", true, markup = inlineKeyboardMarkup)
                    } else {
                        bot.messageResponse(Result.error(InvalidRequest()), msg)
                    }
                } else {
                    bot.sendMessage(msg.chat.id, helpMessage)
                }
            }
        }
    }

    bot.onCommand("/help") { msg, _ ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {
            bot.sendMessage(msg.chat.id, helpMessage)
        }
    }

    fun postCalendarCommand(msg: Message, opts: String?) {
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {
            bot.deleteMessage(msg.chat.id, msg.message_id)
            if (opts != null) {
                val response = commandExecution.executePublishCalendarCommand(opts)
                bot.messageResponse(response, msg)
            } else bot.sendMessage(msg.chat.id, helpMessage)
        }
    }

    bot.onCommand("/postcalendar") { msg, opts ->
        postCalendarCommand(msg, opts)
    }

    bot.onChannelPost { msg ->
        val msgText = msg.text
        if (msgText != null && msgText.startsWith("/postcalendar"))
            postCalendarCommand(msg, msgText.substringAfter(" "))
    }

    bot.start()
}