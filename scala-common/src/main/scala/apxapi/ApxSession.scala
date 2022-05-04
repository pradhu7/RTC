package com.apixio.scala.apxapi

class ApxSession(
    var username: Option[String] = None,
    var password: Option[String] = None,
    var external_token: Option[String] = None,
    var internal_token: Option[String] = None,
    var useraccounts: Option[UserAccounts] = None,
    var tokenizer: Option[Tokenizer] = None) {

  var token_swap_callback: () => Unit = () => {}

  def swap_token() : Unit = {
    val old_token = internal_token
    this.synchronized {
      if (old_token == internal_token) {
        if (external_token.isEmpty) {
          if (internal_token.nonEmpty)
            throw new ApxAuthException("No external token available to perform swap.")
          if (username.isEmpty || password.isEmpty) {
            throw new ApxException("No username/password when attempting to get external token.")
          }
          external_token = Some(useraccounts.get.signin(username.get, password.get))
        }
        try {
          internal_token = Some(tokenizer.get.swap(external_token.get))
          token_swap_callback()
        } catch {
          case e:ApxAuthException =>
            if (username.isEmpty || password.isEmpty)
              throw e
            external_token = Some(useraccounts.get.signin(username.get, password.get))
            internal_token = Some(tokenizer.get.swap(external_token.get))
            token_swap_callback()
        }
      }
    }
  }
}
