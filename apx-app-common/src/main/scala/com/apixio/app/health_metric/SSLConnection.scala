package com.apixio.app.health_metric

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import com.typesafe.config.Config
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object SSLConnection {
  def createHTTPSConnectionContext(appConfig: Config)(implicit system: ActorSystem): Option[HttpsConnectionContext] = {
    if (appConfig.getBoolean("ssl.enable_ssl")) {
      AkkaSSLConfig(system)
      val ks: KeyStore = KeyStore.getInstance(appConfig.getString("ssl.keystore_type"))
      val keystore: InputStream = new FileInputStream(new File(appConfig.getString("ssl.keystore_file"))) //todo: find when to close this.
      val password: Array[Char] = appConfig.getString("ssl.keystore_password").toCharArray
      ks.load(keystore, password)

      val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)

      val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
      tmf.init(ks)

      val sslContext: SSLContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
      Some(ConnectionContext.https(sslContext))
    } else {
      Option.empty
    }
  }
}
