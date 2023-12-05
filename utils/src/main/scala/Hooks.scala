package kr.ac.postech.paranode.utils

import java.net.InetAddress
import java.net.ServerSocket
import scala.util.Using

object Hooks {
  def useLocalHostAddress: String = InetAddress.getLocalHost.getHostAddress
  def useAvailablePort: Int = Using(new ServerSocket(0))(_.getLocalPort).get
}
