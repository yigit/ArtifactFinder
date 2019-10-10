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
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

// extensions for asm ClassNode

private fun List<AnnotationNode>?.hasRestrictTo(): Boolean {
    return this?.any {
        it.desc == "Landroidx/annotation/RestrictTo;"
    } ?: false
}

internal fun ClassNode.isVisibleFromOutside(): Boolean {
    if (outerMethod != null) {
        return false
    }
    if (this.access.and(Opcodes.ACC_PUBLIC) == 0) {
        return false
    }
    if (invisibleAnnotations.hasRestrictTo() || visibleAnnotations.hasRestrictTo()) {
        return false
    }
    return true
}

internal fun ClassNode.isInnerClass() = this.innerClasses?.any {
    it.name == this.name
} ?: false

internal fun ClassNode.toClassInfo() = name.toClassInfo()

internal fun ClassNode.toInnerClassInfo() = InnerClassInfo(
    parent = innerClasses.first().outerName.toClassInfo(),
    classInfo = toClassInfo()
)
