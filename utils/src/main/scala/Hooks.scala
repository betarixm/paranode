package kr.ac.postech.paranode.utils

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import scala.jdk.CollectionConverters._
import scala.reflect.io.Directory
import scala.util.Using

object Hooks {
  def useLocalHostAddress: String =
    NetworkInterface.getNetworkInterfaces.asScala
      .flatMap(_.getInetAddresses.asScala.toList)
      .find(address =>
        address.isInstanceOf[Inet4Address] && !address.isLoopbackAddress
      )
      .get
      .getHostAddress

  def useAvailablePort: Int = Using(new ServerSocket(0))(_.getLocalPort).get
  def useTemporaryDirectory: Directory = Directory.makeTemp("paranode")
}
