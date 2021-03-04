import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.{Listing, ReportServices}
import java.time.Month
class ReportServicesSpec extends AnyWordSpec with Matchers with TestConfigs {


  "ReportServices" when {
    ".averageListingPricePerSellerType" should {
      "return the average price of all listings per sellerType" in {
        val avgPertSeller = reports.averageListingPricePerSellerType
        avgPertSeller.get("private") should contain(35472.67)
      }
    }
    ".averagePriceMostContactedListing" should {
      "return the average price of the top 30% listings by total number of contacts" in {
        val reportByThirtyPercentTopListing = reports.averagePriceMostContactedListing()
        reportByThirtyPercentTopListing shouldBe 29830.0
      }
    }
    ".percentageDistroOfCarsByMake" should {
      "return the percentage of distribution of available cars(listing) grouped by make" in {
          val percentageOfDistro = reports.percentageDistroOfCarsByMake
        percentageOfDistro collect{
          case (make, percentageOfDistro) if make == "Audi" =>
            percentageOfDistro shouldBe 25
          case (make, percentageOfDistro) if make == "Mazda" =>
            percentageOfDistro shouldBe 20
          case (make, percentageOfDistro) if make == "Renault" =>
            percentageOfDistro shouldBe 20
          case (make, percentageOfDistro) if make == "Mercedes-Benz" =>
            percentageOfDistro shouldBe 5
          case (make, percentageOfDistro) if make == "Fiat" =>
            percentageOfDistro shouldBe 10
        }
      }
    }
    ".mostContactedListingByMonth" should {
      "Return The top 5 most contacted listing for each month" in {
        val contactedListing = reports.mostContactedListingByMonth()
        contactedListing collect{
          case (month, listingsAndContacts) if month.getMonth == Month.MAY =>
            listingsAndContacts.size should not be 3
            listingsAndContacts.size shouldBe 5
            listingsAndContacts.head._1.make shouldBe "Mazda"
            listingsAndContacts.head._2 shouldBe 2
        }
      }
    }
  }
}
