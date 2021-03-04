package services

import java.io.File
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

import play.api.Logger
import services.ReportServices.{contactsMarshaller, listingMarshaller, readResource}

import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.{Failure, Success, Try}

sealed trait DataTypes

case class Contact(listingId: Int, contactDate: LocalDateTime) extends DataTypes

case class Listing(id: Int, make: String, price: Double, mileage: Long, sellerType: String) extends DataTypes

case class FileMarshalingException(msg: String) extends Exception(msg)

object UploadFileEnums extends Enumeration {
  type UploadFileEnum = Value
  val CONTACT = Value(1, "contacts")
  val LISTING = Value(2, "listings")
  val UNRECOGNISED = Value(0, "unrecognised")
}

class ReportServices(reportsDir: String)(implicit logger: Logger) {

  private lazy val contacts = readResource(new File(reportsDir + "/contacts.csv"), contactsMarshaller) match {
    case Failure(exception) =>
      throw exception
    case Success(value) =>
      value.asInstanceOf[Seq[Contact]]
  }
  private lazy val listings = readResource(new File(reportsDir + "/listings.csv"), listingMarshaller)match {
    case Failure(exception) =>
      throw exception
    case Success(value) =>
      value.asInstanceOf[Seq[Listing]]
  }

  def avg(seq: Seq[Double]): Double = {
    val res = seq.sum / seq.length
    Try(BigDecimal(res).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .recover{
        case e: Throwable =>
          logger.error(s"an error occur while truncating average to small decimal places: $e")
          0.0
      }
      .getOrElse(0.0)
  }
  def averageListingPricePerSellerType: Map[String, Double] = {
    val listingBySellerTypes = listings.groupBy(_.sellerType).map(e => e._1 -> e._2.map(_.price))
    val average: Map[String, Double] = listingBySellerTypes.map(e => e._1 -> avg(e._2))
    average
  }

  
  def averagePriceMostContactedListing(percentageTop: Double = 30): Double = {
    val listingByContacts = contacts.groupBy(_.listingId).map(e => e._1 -> e._2.length)
    val listingAndContact = listingByContacts.collect{
      case (listingId, contactCount) =>
        listings.find(_.id == listingId).map(e =>  e -> contactCount)
    }.flatten.toVector.sortBy(e => e._2)(Ordering[Int].reverse).map(_._1.price)
    val thirtyPercent = listingAndContact.take((percentageTop / 100 * listingAndContact.length).ceil.toInt)
    avg(thirtyPercent)
  }


  // Get the percentage of cars by each make from all the listings
  def percentageDistroOfCarsByMake: Map[String, Int] = {
    val carsCountByMake = listings.groupBy(_.make).map(e => e._1 -> e._2.length)
    carsCountByMake.map{ makeAndCount =>
      makeAndCount._1 -> makeAndCount._2 * 100 / listings.length
    }
  }

  private def topFiveListingByContactsGroupedByMonth(contacts: Seq[Contact], topCount: Int) = {
    val listingByContacts = contacts.groupBy(_.listingId).map(e => e._1 -> e._2.length)
    val ls = listingByContacts.collect{
      case (listingId, contactCount) =>
       listings.find(_.id == listingId).map(e =>  e -> contactCount)
    }.flatten.toVector.sortBy(e => e._2)(Ordering[Int].reverse).take(topCount)
    contacts.head.contactDate.toLocalDate -> ls
  }

  def mostContactedListingByMonth(topCount: Int = 5): Map[LocalDate, Vector[(Listing, Int)]] = {
    val listingByMonths = contacts
      .groupBy(_.contactDate.getMonth)
      .map(e => topFiveListingByContactsGroupedByMonth(e._2, topCount))
    ListMap(listingByMonths.toSeq.sortBy(_._1.getMonthValue)(Ordering[Int]):_*)
  }

}
object ReportServices{

  def validateFile(file: File)(implicit logger: Logger): (Try[Seq[DataTypes]], UploadFileEnums.Value) = {
    val maybeResource = readResource(file, contactsMarshaller)
    maybeResource match {
      case Failure(_) =>
        readResource(file, listingMarshaller) match {
          case Failure(exception) =>
            Failure(exception) -> UploadFileEnums.UNRECOGNISED
          case Success(value) =>
            Success(value) -> UploadFileEnums.LISTING
        }
      case Success(value) =>
        Success(value) -> UploadFileEnums.CONTACT

    }
  }

  def contactsMarshaller(contacts: Seq[Array[String]] ): Seq[Contact] =
      contacts.map{ contact =>
        Contact(contact(0).toInt, LocalDateTime.ofInstant(Instant.ofEpochMilli(contact(1).toLong), ZoneOffset.UTC) )
      }


  def listingMarshaller(listings: Seq[Array[String]] ): Seq[Listing] = {
      listings.map{ listing =>
        Listing(listing(0).toInt, listing(1), listing(2).toInt, listing(3).toLong, listing(4))
      }
  }

  def readResource(file: File, marshaller: Seq[Array[String]] => Seq[DataTypes])(implicit logger: Logger): Try[Seq[DataTypes]] = {
    if (file.exists()){
      lazy val src = Source.fromFile(file)
      Try{
        val lines =  src.getLines().toList.tail.map { e =>
          val splitted = e.replace(""""""", "").split(",")
          splitted
        }
        marshaller(lines)
      }.recoverWith{
        case e: Throwable =>
          logger.error(s"invalid file or bad data structure: ${e.getMessage} -- caused by: ${e.getCause}")
          Failure(e)
      }
    }else {
      Failure(new RuntimeException(s"File ${file.getName} does not exist in the specified location: ${file.getAbsolutePath}"))
    }

  }
}
