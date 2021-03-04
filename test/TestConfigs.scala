import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import services.ReportServices

trait TestConfigs {

  val config: Config = ConfigFactory.parseFile(new File("test/resources/test.conf")).resolve()
  val dir = config.getString("reportDir")

  val reports = new ReportServices(dir)
}
