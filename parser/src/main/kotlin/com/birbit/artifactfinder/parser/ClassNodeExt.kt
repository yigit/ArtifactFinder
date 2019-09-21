package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.vo.InnerClassInfo
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

// extensions for asm ClassNode

internal fun ClassNode.isVisibleFromOutside(): Boolean {
    if (outerMethod != null) {
        return false
    }
    return this.access.and(Opcodes.ACC_PUBLIC) != 0
}

internal fun ClassNode.isInnerClass() = this.innerClasses?.any {
    it.name == this.name
} ?: false

internal fun ClassNode.toClassInfo() = name.toClassInfo()

internal fun ClassNode.toInnerClassInfo() = InnerClassInfo(
    parent = innerClasses.first().outerName.toClassInfo(),
    classInfo = toClassInfo()
)