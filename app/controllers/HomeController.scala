package controllers

import java.io.File
import java.time.LocalDate

import javax.inject._
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import play.api.mvc._
import services.ReportServices

import scala.util.control.Exception.allCatch
import scala.util.{Failure, Success}

case class FormData(name: String)

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents, configuration: Configuration) extends AbstractController(cc) with I18nSupport {
  implicit val logger = Logger(this.getClass)

  val reportDir = allCatch.opt(configuration.underlying.getString("reportDir"))
  val form: Form[FormData] = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
  )

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index: Action[AnyContent] = Action { implicit request => //TODO add a cache so that we wont compute the same thing all over everytime
    reportDir match {
      case Some(reportDir) =>
        try {
          val report = new ReportServices(reportsDir = reportDir)
          val mostListingsByContacts = report.mostContactedListingByMonth()
          val avgListingsPricePerSeller = report.averageListingPricePerSellerType
          val percentageDistroOfCarsByMake = report.percentageDistroOfCarsByMake
          val avgTopMostContactedListingByprice = report.averagePriceMostContactedListing()
          Ok(views.html.index("Welcome to Auto scout 24",
            mostListingsByContacts = mostListingsByContacts,
            avgListingsPricePerSeller = avgListingsPricePerSeller,
            percentageDistroOfCarsByMake = percentageDistroOfCarsByMake,
            avgTopMostContactedListing = avgTopMostContactedListingByprice,
            form = form))
        } catch {
          case e: Throwable =>
            throw e
            InternalServerError(s"Error: something terrible happened. ${e.getCause}")
        }
      case None =>
        InternalServerError("Something Terrible happened... we could not find the reports directory")
    }

  }

  def api: Action[AnyContent] = Action { implicit request =>
    reportDir match {
      case Some(reportDir) =>
        try {
          val payload = reports(reportDir)
          Ok(payload).as(JSON)
        } catch {
          case e: Throwable =>
            InternalServerError(Json.toJson(JsObject(Map("Error" -> JsString(s"something terrible happened. ${e.getLocalizedMessage}")))))
        }
      case None =>
        InternalServerError(Json.toJson(JsObject(Map("Error" -> JsString(s"Something Terrible happened... we could not find the reports directory")))))
    }
  }

  def reports(reportDir: String): JsObject = {
    val report = new ReportServices(reportsDir = reportDir)
    val mostListingsByContacts = report.mostContactedListingByMonth().map(e => extractMonthAndYear(e._1)  -> e._2)
    val avgListingsPricePerSeller = report.averageListingPricePerSellerType
    val percentageDistroOfCarsByMake = report.percentageDistroOfCarsByMake
    val avgTopMostContactedListingByprice = report.averagePriceMostContactedListing()
    val mm = mostListingsByContacts.map(e =>
      JsObject(Map(e._1 -> JsArray(e._2.map(e => JsObject(Map(e._1.id.toString -> JsNumber(e._2)))))))
    ).toSeq
    val avg = avgListingsPricePerSeller.map(e => JsObject(Map(e._1 -> JsNumber(e._2)))).toSeq
    val perce = percentageDistroOfCarsByMake.map(e => JsObject(Map(e._1 -> JsNumber(e._2)))).toSeq
    JsObject(
      Seq(
        "mostListingsByContacts" -> JsArray(mm),
        "avgListingsPricePerSeller" -> JsArray(avg),
        "percentageDistroOfCarsByMake" -> JsArray(perce),
        "avgTopMostContactedListingByPrice" -> JsNumber(avgTopMostContactedListingByprice)
      )
    )
  }

  def uploadReportFile = Action(parse.multipartFormData) { implicit request =>
    request.body.file("report").map { reportFile =>
      reportFile.contentType match {
        case Some(value) if value == "text/csv" =>
          ReportServices.validateFile(reportFile.ref) match {
            case (Failure(exception), _) =>
              Redirect(routes.HomeController.index())
                .flashing("File upload failed" -> s"Invalid file: ${exception.getMessage} ")
            case (Success(_), typ) =>
              reportFile.ref.moveTo(new File(s"${reportDir.getOrElse("conf/reportDir")}/${typ.toString}.csv"), true)
              Redirect(routes.HomeController.index()).withNewSession
          }
        case _ =>
          Redirect(routes.HomeController.index()).flashing("File upload failed" -> "Invalid file format, please kindly try csv")
      }
    }.getOrElse {
      Redirect(routes.HomeController.index()).flashing("File upload failed" -> " File cannot be found in payload")
    }
  }

  def extractMonthAndYear(date: LocalDate) = s"${date.getMonthValue}-${date.getYear}"
}
