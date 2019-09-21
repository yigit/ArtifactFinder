package com.birbit.artifactfinder.maven

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class JacksonConverterFactory : Converter.Factory() {
    private val mapper = XmlMapper().registerModule(KotlinModule()).registerModule(
        JacksonXmlModule()
    )
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun responseBodyConverter(
        type: Type, annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return when (type) {
            is Class<*> -> JacksonXmlBodyConverter(mapper, type)
            is ParameterizedType -> JacksonXmlBodyConverter(mapper, type.rawType as Class<*>)
            else -> null
        }
    }

    class JacksonXmlBodyConverter<T>(
        private val mapper: ObjectMapper,
        private val klass: Class<T>
    ) : Converter<ResponseBody, T> {
        override fun convert(value: ResponseBody?): T? {
            return value?.let {
                mapper.readValue(it.charStream(), klass)
            }
        }
    }
}
