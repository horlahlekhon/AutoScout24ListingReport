import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger
import services.ReportServices

trait TestConfigs {
  implicit val logger = Logger(this.getClass)
  val config: Config = ConfigFactory.parseFile(new File("test/resources/test.conf")).resolve()
  val dir = config.getString("reportDir")

  val reports = new ReportServices(dir)
}
