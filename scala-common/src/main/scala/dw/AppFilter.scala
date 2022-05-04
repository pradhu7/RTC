package com.apixio.scala.dw

import javax.servlet._

import org.eclipse.jetty.server.Response

class AppFilter(fn: (ServletRequest, ServletResponse) => Unit) extends Filter {
  override def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    fn(req, res)

    res.asInstanceOf[Response].getStatus < 400 match {
      case true => chain.doFilter(req, res) // Keep going, if there are no errors
      case _ => return // We are done here
    }
  }

  override def destroy() {} // Implementation required but not used

  override def init(filterConfig: FilterConfig): Unit = {} // Implementation required but not used
}
