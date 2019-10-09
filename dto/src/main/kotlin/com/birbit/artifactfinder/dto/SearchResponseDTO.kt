package com.birbit.artifactfinder.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDTO(
    val latestVersion: Int,
    val results: List<SearchResultDTO>
)