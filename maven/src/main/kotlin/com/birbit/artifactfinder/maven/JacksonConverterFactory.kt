/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birbit.artifactfinder.maven

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

internal class JacksonConverterFactory : Converter.Factory() {
    private val mapper = XmlMapper().registerModule(KotlinModule()).registerModule(
        JacksonXmlModule()
    )
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
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
