#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.1.0")
@file:DependsOn("com.google.auth:google-auth-library-oauth2-http:1.54.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Creates or updates the supporter subscriptions and tips on Google Play from a config file: prices,
 * localized titles, descriptions and benefits. Running it again updates what's there.
 *
 * Each price is used as is in the US and in every euro country. Google Play converts it for all other
 * countries, the same way the Play Console does.
 */
class PlayProducts : CliktCommand() {

  private val config by option(help = "The products, prices and listings")
    .file(mustExist = true)
    .default(File("fastlane/play_products.json"))
  private val serviceAccount by option(help = "Service account with access to the Play Console")
    .file(mustExist = true)
    .default(File("app/play_service_account.json"))
  private val dryRun by option(help = "Only converts the prices and writes what would be uploaded to build/play_products")
    .flag()

  override fun help(context: Context): String = "Creates or updates the supporter subscriptions and tips on Google Play"

  override fun run() {
    val config = Json.parseToJsonElement(config.readText()).jsonObject
    val packageName = config.string("packageName")
    val listings = config.getValue("listings").jsonObject.mapValues { it.value.jsonObject }
    validate(config, listings)

    val api = PlayApi(serviceAccount)
    val prices = mutableMapOf<BigDecimal, Prices>()
    fun prices(price: BigDecimal) = prices.getOrPut(price) { api.convert(packageName, price) }
    val outDir = File("build/play_products")
    if (dryRun) outDir.mkdirs()

    config.getValue("subscriptions").jsonArray.map { it.jsonObject }.forEach { product ->
      val productId = product.string("productId")
      val basePlanId = product.string("basePlanId")
      val price = prices(BigDecimal(product.string("price")))
      val body = subscription(packageName, product, listings, price)
      echo("$productId: ${price.summary()}")
      if (dryRun) {
        File(outDir, "$productId.json").writeText(body.toPrettyString())
        return@forEach
      }
      val subscription = api.request(
        method = "PATCH",
        path = "applications/$packageName/subscriptions/$productId",
        query = mapOf(
          "updateMask" to "listings,basePlans",
          "allowMissing" to "true",
          "regionsVersion.version" to price.regionsVersion,
        ),
        body = body,
      )
      val state = subscription.getValue("basePlans").jsonArray
        .map { it.jsonObject }
        .single { it.string("basePlanId") == basePlanId }
        .string("state")
      if (state != "ACTIVE") {
        api.request(
          method = "POST",
          path = "applications/$packageName/subscriptions/$productId/basePlans/$basePlanId:activate",
          body = buildJsonObject {
            put("packageName", packageName)
            put("productId", productId)
            put("basePlanId", basePlanId)
          },
        )
        echo("  activated $basePlanId")
      }
    }

    config.getValue("tips").jsonArray.map { it.jsonObject }.forEach { product ->
      val productId = product.string("productId")
      val price = prices(BigDecimal(product.string("price")))
      val body = tip(packageName, product, listings, price)
      echo("$productId: ${price.summary()}")
      if (dryRun) {
        File(outDir, "$productId.json").writeText(body.toPrettyString())
        return@forEach
      }
      val tip = api.request(
        method = "PATCH",
        path = "applications/$packageName/onetimeproducts/$productId",
        query = mapOf(
          "updateMask" to "listings,purchaseOptions",
          "allowMissing" to "true",
          "regionsVersion.version" to price.regionsVersion,
        ),
        body = body,
      )
      val state = tip.getValue("purchaseOptions").jsonArray
        .map { it.jsonObject }
        .single { it.string("purchaseOptionId") == TIP_PURCHASE_OPTION }
        .string("state")
      if (state != "ACTIVE") {
        api.request(
          method = "POST",
          path = "applications/$packageName/oneTimeProducts/$productId/purchaseOptions:batchUpdateStates",
          body = buildJsonObject {
            putJsonArray("requests") {
              addJsonObject {
                putJsonObject("activatePurchaseOptionRequest") {
                  put("packageName", packageName)
                  put("productId", productId)
                  put("purchaseOptionId", TIP_PURCHASE_OPTION)
                }
              }
            }
          },
        )
        echo("  activated $TIP_PURCHASE_OPTION")
      }
    }
    if (dryRun) echo("Nothing was uploaded. The requests are in ${outDir.path}")
  }

  private fun subscription(
    packageName: String,
    product: JsonObject,
    listings: Map<String, JsonObject>,
    prices: Prices,
  ) = buildJsonObject {
    put("packageName", packageName)
    put("productId", product.string("productId"))
    putJsonArray("listings") {
      listings.forEach { (language, texts) ->
        val listing = texts.getValue(product.string("listing")).jsonObject
        addJsonObject {
          put("languageCode", language)
          put("title", listing.string("title"))
          put("description", listing.string("description"))
          put("benefits", texts.getValue("benefits"))
        }
      }
    }
    putJsonArray("basePlans") {
      addJsonObject {
        put("basePlanId", product.string("basePlanId"))
        putJsonObject("autoRenewingBasePlanType") {
          put("billingPeriodDuration", product.string("billingPeriod"))
          put("resubscribeState", "RESUBSCRIBE_STATE_ACTIVE")
          put("legacyCompatible", true)
        }
        putJsonArray("regionalConfigs") {
          prices.regions.forEach { (regionCode, price) ->
            addJsonObject {
              put("regionCode", regionCode)
              put("newSubscriberAvailability", true)
              put("price", price)
            }
          }
        }
        putJsonObject("otherRegionsConfig") {
          put("newSubscriberAvailability", true)
          put("usdPrice", prices.otherRegionsUsd)
          put("eurPrice", prices.otherRegionsEur)
        }
      }
    }
  }

  private fun tip(
    packageName: String,
    product: JsonObject,
    listings: Map<String, JsonObject>,
    prices: Prices,
  ) = buildJsonObject {
    put("packageName", packageName)
    put("productId", product.string("productId"))
    putJsonArray("listings") {
      listings.forEach { (language, texts) ->
        val listing = texts.getValue(product.string("listing")).jsonObject
        addJsonObject {
          put("languageCode", language)
          put("title", listing.string("title"))
          put("description", listing.string("description"))
        }
      }
    }
    putJsonArray("purchaseOptions") {
      addJsonObject {
        put("purchaseOptionId", TIP_PURCHASE_OPTION)
        putJsonObject("buyOption") {
          // the app reads the price through the backwards compatible purchase option
          put("legacyCompatible", true)
          put("multiQuantityEnabled", false)
        }
        putJsonArray("regionalPricingAndAvailabilityConfigs") {
          prices.regions.forEach { (regionCode, price) ->
            addJsonObject {
              put("regionCode", regionCode)
              put("price", price)
              put("availability", "AVAILABLE")
            }
          }
        }
        putJsonObject("newRegionsConfig") {
          put("usdPrice", prices.otherRegionsUsd)
          put("eurPrice", prices.otherRegionsEur)
          put("availability", "AVAILABLE")
        }
      }
    }
  }

  /** Fails before anything is uploaded if a text is missing or longer than Google Play allows. */
  private fun validate(
    config: JsonObject,
    listings: Map<String, JsonObject>,
  ) {
    val errors = mutableListOf<String>()
    fun checkLength(
      what: String,
      text: String,
      max: Int,
    ) {
      val length = text.codePointCount(0, text.length)
      if (length > max) errors += "$what is $length characters long, at most $max are allowed: $text"
    }
    if (DEFAULT_LANGUAGE !in listings) errors += "The listings miss the default language $DEFAULT_LANGUAGE"
    listings.forEach { (language, texts) ->
      val benefits = texts["benefits"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
      if (benefits.size > 4) errors += "$language has ${benefits.size} benefits, at most 4 are allowed"
      benefits.forEach { checkLength("A $language benefit", it, max = 40) }
      listOf("subscriptions" to 80, "tips" to 200).forEach { (products, maxDescription) ->
        config.getValue(products).jsonArray.map { it.jsonObject.string("listing") }.distinct().forEach { key ->
          val listing = texts[key]?.jsonObject
          if (listing == null) {
            errors += "$language misses the listing $key"
          } else {
            checkLength("The $language title of $key", listing.string("title"), max = 55)
            checkLength("The $language description of $key", listing.string("description"), max = maxDescription)
          }
        }
      }
    }
    check(errors.isEmpty()) { errors.joinToString(separator = "\n") }
  }

  private companion object {
    const val DEFAULT_LANGUAGE = "en-US"
    const val TIP_PURCHASE_OPTION = "buy"
  }
}

/** The prices for every region, converted from one price. */
class Prices(
  val regionsVersion: String,
  val regions: List<Pair<String, JsonObject>>,
  val otherRegionsUsd: JsonObject,
  val otherRegionsEur: JsonObject,
) {

  fun summary(): String {
    val prices = regions.toMap()
    return listOf("US", "DE", "GB", "CH", "JP", "IN", "BR")
      .mapNotNull { region -> prices[region]?.let { "$region ${it.format()}" } }
      .joinToString() + ", ${regions.size} regions"
  }
}

class PlayApi(serviceAccount: File) {

  private val credentials = serviceAccount.inputStream()
    .use { GoogleCredentials.fromStream(it) }
    .createScoped(listOf("https://www.googleapis.com/auth/androidpublisher"))
  private val client = HttpClient.newHttpClient()

  /** Converts a price in USD the way the Play Console does, and uses it as is in euro countries. */
  fun convert(
    packageName: String,
    price: BigDecimal,
  ): Prices {
    val response = request(
      method = "POST",
      path = "applications/$packageName/pricing:convertRegionPrices",
      body = buildJsonObject { put("price", money(price, "USD")) },
    )
    val euro = money(price, "EUR")
    val regions = response.getValue("convertedRegionPrices").jsonObject.values
      .map { it.jsonObject }
      .map { converted ->
        val regionPrice = converted.getValue("price").jsonObject
        converted.string("regionCode") to if (regionPrice.string("currencyCode") == "EUR") euro else regionPrice
      }
      .sortedBy { it.first }
    val otherRegions = response.getValue("convertedOtherRegionsPrice").jsonObject
    return Prices(
      regionsVersion = response.getValue("regionVersion").jsonObject.string("version"),
      regions = regions,
      otherRegionsUsd = otherRegions.getValue("usdPrice").jsonObject,
      otherRegionsEur = otherRegions.getValue("eurPrice").jsonObject,
    )
  }

  fun request(
    method: String,
    path: String,
    query: Map<String, String> = emptyMap(),
    body: JsonObject,
  ): JsonObject {
    credentials.refreshIfExpired()
    val queryString = query.entries.joinToString(separator = "&") { (key, value) ->
      "$key=${URLEncoder.encode(value, Charsets.UTF_8)}"
    }
    val uri = URI("https://androidpublisher.googleapis.com/androidpublisher/v3/$path" + if (queryString.isEmpty()) "" else "?$queryString")
    val request = HttpRequest.newBuilder(uri)
      .header("Authorization", "Bearer ${credentials.accessToken!!.tokenValue}")
      .header("Content-Type", "application/json; charset=utf-8")
      .method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() in 200..299) {
      "$method $path failed with ${response.statusCode()}:\n${response.body()}"
    }
    return Json.parseToJsonElement(response.body().ifBlank { "{}" }).jsonObject
  }
}

fun money(
  amount: BigDecimal,
  currencyCode: String,
): JsonObject = buildJsonObject {
  put("currencyCode", currencyCode)
  put("units", amount.toBigInteger().toString())
  put("nanos", amount.remainder(BigDecimal.ONE).movePointRight(9).toInt())
}

/** Money in JSON leaves out units and nanos that are zero. */
fun JsonObject.format(): String {
  val units = get("units")?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO
  val nanos = get("nanos")?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO
  return "${(units + nanos.movePointLeft(9)).stripTrailingZeros().toPlainString()} ${string("currencyCode")}"
}

fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

fun JsonElement.toPrettyString(): String = Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), this)

PlayProducts().main(args)
