package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.response
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val moshiInstance: Moshi = Moshi.Builder()
        .add(YearMonthJsonAdapter())
        .add(LocalDateJsonAdapter())
        .add(URIJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalJsonAdapter())
        .build()!!

class YearMonthJsonAdapter {
    @ToJson
    fun toJson(yearMonth: YearMonth): String {
        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    @FromJson
    fun fromJson(json: String): YearMonth {
        return YearMonth.parse(json)
    }
}

class LocalDateJsonAdapter {
    @ToJson
    fun toJson(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @FromJson
    fun fromJson(json: String): LocalDate {
        return LocalDate.parse(json)
    }
}

class URIJsonAdapter {
    @ToJson
    fun toJson(uri: URI): String {
        return uri.toString()
    }

    @FromJson
    fun fromJson(json: String): URI {
        return URI.create(json)
    }
}

class BigDecimalJsonAdapter {

    @ToJson
    fun toJson(bigDecimal: BigDecimal): String {
        return bigDecimal.toString()
    }

    @FromJson
    fun fromJson(json: String): BigDecimal {
        return BigDecimal(json)
    }
}

internal fun <T : Any> moshiDeserializerOf(clazz: Class<T>) = object : ResponseDeserializable<T> {
    override fun deserialize(content: String): T? = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(clazz)
        .fromJson(content)
}

internal inline fun <reified T : Any> Request.responseObject() = response(moshiDeserializerOf(T::class.java))
