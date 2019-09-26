package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.InnerClassInfo
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

// extensions for asm ClassNode

private fun List<AnnotationNode>?.hasRestrictTo() : Boolean {
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