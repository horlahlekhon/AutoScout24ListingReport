import java.nio.file.Files

import controllers.HomeController
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import play.api.{Application, Configuration}

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerSuite {
  val configuration = app.injector.instanceOf[Configuration]
  val controller = new HomeController(Helpers.stubMessagesControllerComponents(), configuration)

  override def fakeApplication: Application = new GuiceApplicationBuilder()
    .configure(
      "reportDir" -> "test/resources/"
    )
    .build()

  "HomeController GET /" should {
    "be valid and return valid response" in {
      val resp = controller.index().apply(FakeRequest(GET, "/").withCSRFToken)
      status(resp) mustBe OK
      contentType(resp) mustBe Some("text/html")
      contentAsString(resp) must include("Percentual distribution of available cars by Make")
    }
  }
  "HomeController POST /" should {
    "upload file and redirect back to index" in {
      val fileToUpload = play.api.libs.Files.SingletonTemporaryFileCreator.create("upload", "csv")
      fileToUpload.deleteOnExit()
      val data =
        """"listing_id","contact_date"
          |1244,1614659652000
          |1244,1619930052000
          |1244,1622608452000""".stripMargin
      Files.write(fileToUpload.toPath, data.getBytes())
      val file = FilePart("report", "contacts.csv", Option("text/csv"), fileToUpload)
      val formData = MultipartFormData(
        dataParts = Map[String, Seq[String]](),
        files = Seq(file),
        badParts = Seq()
      )
      val upload = controller.uploadReportFile().apply(FakeRequest(POST, "/", FakeHeaders(), formData))
      println(contentAsString(upload))
      status(upload) mustBe 303
      redirectLocation(upload) mustBe Some("/")
      flash(upload).get("File upload failed") mustBe None
    }

    "reject file upload for invalid structured file" in {
      val fileToUpload = play.api.libs.Files.SingletonTemporaryFileCreator.create("upload", "csv")
      fileToUpload.deleteOnExit()
      val data =
        """
          |quick brown fox jumps
          |
          |
          |1244,1614659652000
          |1244,1619930052000
          |1244,1622608452000""".stripMargin
      Files.write(fileToUpload.toPath, data.getBytes())
      val file = FilePart("report", "contacts.csv", Option("text/csv"), fileToUpload)
      val formData = MultipartFormData(
        dataParts = Map[String, Seq[String]]("key" -> Seq("value")),
        files = Seq(file),
        badParts = Seq()
      )
      val upload = controller.uploadReportFile().apply(FakeRequest(POST, "/", FakeHeaders(), formData))
      redirectLocation(upload) mustBe Some("/")
      flash(upload).get("File upload failed").get must include("Invalid file:")
    }
    "reject non csv file" in {
      val fileToUpload = play.api.libs.Files.SingletonTemporaryFileCreator.create("upload", "pdf")
      fileToUpload.deleteOnExit()
      val data =
        """
          |quick brown fox jumps
          |
          |
          |1244,1614659652000
          |1244,1619930052000
          |1244,1622608452000""".stripMargin
      Files.write(fileToUpload.toPath, data.getBytes())
      val file = FilePart("report", "contacts.csv", Option("text/plain"), fileToUpload)
      val formData = MultipartFormData(
        dataParts = Map[String, Seq[String]]("key" -> Seq("value")),
        files = Seq(file),
        badParts = Seq()
      )
      val upload = controller.uploadReportFile().apply(FakeRequest(POST, "/", FakeHeaders(), formData))
      redirectLocation(upload) mustBe Some("/")
      flash(upload).get("File upload failed").get must include("Invalid file format, please kindly try csv")
    }
  }
  "HomeController GET /api" should {
    "response with content type application/json" in {
      val resp = controller.api().apply(FakeRequest(GET, "/api"))
      status(resp) mustBe OK
      contentType(resp) mustBe Some("application/json")
    }
  }

}
