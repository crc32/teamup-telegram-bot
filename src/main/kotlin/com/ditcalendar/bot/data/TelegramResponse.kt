package com.ditcalendar.bot.data

sealed class TelegramResponse(val message: String, val callbackNotificationText: String?)
class WithMessage(message: String, callbackNotificationText: String?) : TelegramResponse(message, callbackNotificationText)
class WithInline(message: String, val callBackText: String, val callBackData: String,
                 callbackNotificationText: String?) : TelegramResponse(message, callbackNotificationText)