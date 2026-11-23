package ch.opum.tricktrack

import ch.opum.tricktrack.data.PhotonAddressFormatter
import ch.opum.tricktrack.data.PhotonPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotonAddressFormatterTest {

    private fun place(
        name: String? = null,
        street: String? = null,
        housenumber: String? = null,
        postcode: String? = "9001",
        city: String? = "St. Gallen",
        type: String? = "house",
        lat: Double = 47.4232,
        lon: Double = 9.3697
    ) = PhotonPlace(name, street, housenumber, postcode, city, type, lat, lon)

    @Test
    fun formatsFullAddressLikeTheOldGeocoder() {
        val p = place(street = "Bahnhofplatz", housenumber = "2")
        assertEquals("Bahnhofplatz 2, 9001 St. Gallen", PhotonAddressFormatter.format(p))
    }

    @Test
    fun formatsWithoutHouseNumberOrPostcode() {
        assertEquals("Bahnhofplatz, 9001 St. Gallen", PhotonAddressFormatter.format(place(street = "Bahnhofplatz")))
        assertEquals("Bahnhofplatz, St. Gallen", PhotonAddressFormatter.format(place(street = "Bahnhofplatz", postcode = null)))
        assertEquals("Bahnhofplatz", PhotonAddressFormatter.format(place(street = "Bahnhofplatz", city = null)))
    }

    @Test
    fun ignoresPoiNameAndUsesStreet() {
        // A shop's name is not part of the address.
        val p = place(name = "Klubschule Migros", street = "Bahnhofplatz")
        assertEquals("Bahnhofplatz, 9001 St. Gallen", PhotonAddressFormatter.format(p))
    }

    @Test
    fun streetResultUsesNameAsStreet() {
        val p = place(name = "Eisengasse", type = "street", postcode = "4051", city = "Basel")
        assertEquals("Eisengasse, 4051 Basel", PhotonAddressFormatter.format(p))
    }

    @Test
    fun placeResultUsesNameAsCity() {
        val p = place(name = "Appenzell", type = "village", postcode = "9050", city = null)
        assertEquals("9050 Appenzell", PhotonAddressFormatter.format(p))
    }

    @Test
    fun returnsNullWhenNothingUsable() {
        assertNull(PhotonAddressFormatter.format(null))
        assertNull(PhotonAddressFormatter.format(place(name = "Some Shop", postcode = null, city = null)))
    }

    @Test
    fun picksNearbyHouseNumberOverNearerPoiWithout() {
        val poi = place(name = "Klubschule Migros", street = "Bahnhofplatz") // at the query point
        val house = place(street = "Bahnhofplatz", housenumber = "2", lat = 47.4234, lon = 9.3698) // ~25 m away
        val best = PhotonAddressFormatter.pickBestReverse(listOf(poi, house), 47.4232, 9.3697)
        assertEquals("2", best?.housenumber)
    }

    @Test
    fun ignoresHouseNumberTooFarAway() {
        val poi = place(name = "Klubschule Migros", street = "Bahnhofplatz")
        val farHouse = place(street = "Bahnhofplatz", housenumber = "9", lat = 47.4242, lon = 9.3697) // ~110 m away
        val best = PhotonAddressFormatter.pickBestReverse(listOf(farHouse, poi), 47.4232, 9.3697)
        assertNull(best?.housenumber)
        assertEquals("Bahnhofplatz", best?.street)
    }

    @Test
    fun fallsBackToNearestWhenNoStreet() {
        val village = place(name = "Appenzell", type = "village", city = null)
        assertEquals(village, PhotonAddressFormatter.pickBestReverse(listOf(village), 47.4232, 9.3697))
        assertNull(PhotonAddressFormatter.pickBestReverse(emptyList(), 47.4232, 9.3697))
    }

    @Test
    fun distanceIsRoughlyCorrect() {
        // St. Gallen station to Zurich HB is about 62 km in a straight line.
        val d = PhotonAddressFormatter.distanceMeters(47.4232, 9.3697, 47.3779, 8.5403)
        assertEquals(62_500.0, d, 1_500.0)
    }
}
