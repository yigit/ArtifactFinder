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

package com.birbit.artifactfinder.model

/**
 * Represents the lookup information for a class.
 *
 * For instance, if the class is called "Foo$Bar", we'll two entries in this table that has
 * identifier "Bar" and "Foo$Bar" that refers back to the classId.
 *
 * Identifiers are always lowercase so that we can do fast lookup w/ case-insensitive like
 *
 * The identifier is indexed, which allows us to do a query on it with prefix-search
 */
data class ClassLookup(
    val identifier: String,
    val classId: Long
)
