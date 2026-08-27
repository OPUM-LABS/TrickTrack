package ch.opum.tricktrack.data

import android.annotation.SuppressLint
import android.content.Context
import java.text.Normalizer
import java.util.Locale

object CarBrandHelper {
    private val iconCache = mutableMapOf<String, Int>()

    val brands = listOf(
        "Abarth", "Acura", "Alfa Romeo", "Aston Martin", "Audi", "Bentley", "BMW", "Bugatti", "Buick",
        "Cadillac", "Chevrolet", "Chrysler", "Citroën", "Dacia", "Daewoo", "Daihatsu", 
        "Dodge", "Eagle", "Ferrari", "Fiat", "Fisker", "Ford", "Genesis", "GMC", "Honda", "Hummer",
        "Hyundai", "Infiniti", "Isuzu", "Jaguar", "Jeep", "Kia", "Koenigsegg", 
        "Lamborghini", "Lancia", "Land Rover", "Lexus", "Lincoln", "Lotus", 
        "Maserati", "Maybach", "Mazda", "McLaren", "Mercedes-Benz", "Mercury", "MG", "Mini",
        "Mitsubishi", "Nissan", "Oldsmobile", "Opel", "Pagani", "Peugeot", "Plymouth", "Polestar", "Pontiac", "Porsche",
        "Ram", "Renault", "Rolls-Royce", "Rover", "Saab", "Scion", "Seat", "Skoda", "Smart",
        "SsangYong", "Subaru", "Suzuki", "Tesla", "Toyota", "Vauxhall", "Volkswagen", 
        "Volvo"
    ).sorted()

    @SuppressLint("DiscouragedApi")
    fun getBrandIconResId(context: Context, brandName: String): Int {
        if (brandName.isBlank()) return 0
        
        val normalized = Normalizer.normalize(brandName, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        if (normalized.isEmpty()) return 0
        
        // Return cached ID if available
        iconCache[normalized]?.let { return it }

        val resId = context.resources.getIdentifier(
            "ic_brand_$normalized", 
            "drawable", 
            context.packageName
        )
        
        if (resId != 0) {
            iconCache[normalized] = resId
        }
        
        return resId
    }
}
