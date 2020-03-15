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

package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.InnerClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedMethodInfo
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmType

// extensions for KotlinMetadata classes

private val unwantedFlags = arrayOf(
    Flag.IS_INTERNAL,
    Flag.IS_LOCAL,
    Flag.IS_PRIVATE_TO_THIS,
    Flag.IS_PROTECTED,
    Flag.IS_LOCAL,
    Flag.IS_PRIVATE
)

private fun Flags.isVisibleFromOutsideFlags() = unwantedFlags.none {
    it.invoke(this)
}

internal fun KmType.toClassInfo() = this.classifier.let {
    when (it) {
        is KmClassifier.Class -> it.name.toClassInfo()
        is KmClassifier.TypeAlias -> it.name.toClassInfo()
        is KmClassifier.TypeParameter -> {
            throw IllegalArgumentException("$this cannot become a class info")
        }
    }
}

internal fun KmClass.isVisibleFromOutside() = flags.isVisibleFromOutsideFlags()

internal fun KmFunction.isVisibleFromOutside() = flags.isVisibleFromOutsideFlags()

internal fun KmClass.toClassInfo() = this.name.toClassInfo()

internal fun KmClass.isInnerClass() = this.name.contains('.')

internal fun KmClass.toInnerClassInfo(): InnerClassInfo {
    val dotIndex = name.lastIndexOf('.')
    check(dotIndex >= 0) {
        "$this should have had . in its name: $name"
    }
    val parent = name.substring(0, dotIndex)
    return InnerClassInfo(
        parent = parent.toClassInfo(),
        classInfo = toClassInfo()
    )
}

internal fun KmFunction.isExtensionMethod() = this.receiverParameterType != null

internal fun KmFunction.toGlobalFunction(pkg: String) = ParsedMethodInfo(
    pkg = pkg,
    receiver = null,
    name = name)

internal fun KmFunction.toExtensionFunction(pkg: String) = ParsedMethodInfo(
    pkg = pkg,
    receiver = receiverParameterType!!.toClassInfo(),
    name = name
)
