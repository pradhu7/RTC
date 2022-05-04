package com.apixio.scala.apxapi

import com.apixio.scala.dw.ApxConfiguration
import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers


@DoNotDiscover
class RouterSpec extends AuthSpec with Matchers {
  val apxapi = login(custops)

  "Router" should "get all queue status" in {
    val queueStatus = ApxApi.internal.router.statusQueue()
    println(queueStatus)

    queueStatus.nonEmpty shouldBe true
  }

  it should "get 1 queue status" in {
    val queueStatus = ApxApi.internal.router.statusQueue(Some("PRHCC_f4d2de51-44cf-47e8-b203-0db4c969201a,code,Collector"))
    println(queueStatus)

    queueStatus.nonEmpty shouldBe true
  }

  it should "get project remaining" in {
    val remainings = ApxApi.internal.router.projectRemaining("PRHCC_f4d2de51-44cf-47e8-b203-0db4c969201a")
    println(remainings)

    remainings.nonEmpty shouldBe true
  }
}
