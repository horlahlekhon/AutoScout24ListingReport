import java.io.File
import java.nio.file.Files

import controllers.HomeController
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerSuite}
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.api.test.Helpers.{GET, await, contentAsString, contentType, defaultAwaitTimeout, status}
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.ws.WSClient
import play.api.mvc._

class HomeControllerIntergrationSpec extends PlaySpec with GuiceOneServerPerSuite with Injecting{



  "HomeController GET /" should {
    "render report" in {
      val url = s"http://localhost:${port}/"
      val home = inject[WSClient].url(url).get()
      val resp = await(home)
      resp.status mustBe OK
      resp.contentType mustBe "text/html; charset=UTF-8"
      resp.body must include ("Percentual distribution of available cars by Make")
    }
  }
  "HomeController POST /" should {
    "upload a file and validate it" in {
      val fileToUpload = File.createTempFile("upload", "csv")
      fileToUpload.deleteOnExit()
      val data =
        """"listing_id","contact_date"
          |1244,1614659652000
          |1244,1619930052000
          |1244,1622608452000""".stripMargin
      Files.write(fileToUpload.toPath, data.getBytes())
      val url = s"http://localhost:${port}/"
      val responseFuture = inject[WSClient].url(url).post(postSource(fileToUpload))
      val response = await(responseFuture)
      response.status mustBe OK
    }
  }

  "HomeController GET /api" should {
    "get report in json format" in  {
      val url = s"http://localhost:${port}/api"
      val home = inject[WSClient].url(url).get()
      val resp = await(home)
      resp.status mustBe OK
      resp.contentType mustBe "application/json"
    }
  }

  def postSource(tmpFile: File): Source[MultipartFormData.Part[Source[ByteString, _]], _] = {
    import play.api.mvc.MultipartFormData._
    Source(FilePart("report", "contacts.csv", Option("text/csv"),
      FileIO.fromPath(tmpFile.toPath)) :: DataPart("key", "value") :: List())
  }
}
