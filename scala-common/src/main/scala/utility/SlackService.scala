package com.apixio.scala.utility

import com.apixio.scala.dw.ApxConfiguration
import com.apixio.scala.logging.ApixioLoggable
import com.flyberrycapital.slack.SlackClient
import com.flyberrycapital.slack.Responses.PostMessageResponse

import scala.util.{Failure, Success, Try}

class SlackService(config: Map[String, String]) extends ApixioLoggable {
  // Logger
  setupLog(getClass.getCanonicalName)

  private val apiToken = config.getOrElse("apiToken",
                                          throw new RuntimeException("Slack config not found"))

  private val slackClient = new SlackClient(apiToken)

  def postMessage(channel: String, msg: String): PostMessageResponse = {
    slackClient.chat.postMessage(channel, msg)
  }

  def updateMessage(msgPointer: PostMessageResponse, newMsg: String) = {
    slackClient.chat.update(msgPointer, newMsg)
  }
}
