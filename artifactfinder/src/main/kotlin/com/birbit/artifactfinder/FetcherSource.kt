package com.birbit.artifactfinder

import com.birbit.artifactfinder.vo.Artifactory

data class FetcherSource(
    val artifacts: List<ArtifactSource>,
    val groups: List<GroupSource>
)

data class GroupSource(
    val groupId: String,
    val artifactory: Artifactory
)

data class ArtifactSource(
    val groupId: String,
    val artifactId: String,
    val artifactory: Artifactory,
    val processorCoordinates: String? = null
)
