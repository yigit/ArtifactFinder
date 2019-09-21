package com.birbit.artifactfinder.maven

import com.birbit.artifactfinder.maven.vo.ArtifactMetadata
import com.birbit.artifactfinder.maven.vo.Artifactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

internal interface MavenApi {
    companion object {
        internal const val MAVEN_METADATA = "maven-metadata.xml"
        internal const val GROUP_INDEX = "group-index.xml"
        internal const val MASTER_INDEX = "master-index.xml"
        private fun create(baseUrl: HttpUrl): MavenApi {
            val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory())
                .build()
            return builder.create(MavenApi::class.java)
        }

        fun create(artifactory: Artifactory, baseUrl: HttpUrl? = null): MavenApi {
            val httpUrl = baseUrl ?: artifactory.baseUrl.toHttpUrlOrNull()
            return httpUrl?.let {
                create(it)
            } ?: throw IllegalArgumentException("bad url ${artifactory.baseUrl}")
        }
    }

    @GET("{groupPath}/{artifactId}/$MAVEN_METADATA")
    suspend fun mavenMetadata(
        @Path(value = "groupPath", encoded = true) groupPath: String,
        @Path("artifactId") artifactId: String
    ): ArtifactMetadata

    @GET("{groupPath}/{artifactId}/{version}/{artifactId}-{version}.jar")
    suspend fun jar(
        @Path(value = "groupPath", encoded = true) groupPath: String,
        @Path("artifactId") artifactId: String,
        @Path("version") version: String
    ): ResponseBody

    @GET("{groupPath}/{artifactId}/{version}/{artifactId}-{version}.aar")
    suspend fun aar(
        @Path(value = "groupPath", encoded = true) groupPath: String,
        @Path("artifactId") artifactId: String,
        @Path("version") version: String
    ): ResponseBody

    @GET(MASTER_INDEX)
    suspend fun masterIndex(): Map<String, String>

    @GET("{groupPath}/$GROUP_INDEX")
    suspend fun groupIndex(@Path("groupPath", encoded = true) groupPath: String)
            : Map<String, String>
}
