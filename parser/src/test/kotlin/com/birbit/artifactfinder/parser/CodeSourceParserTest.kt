package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.testapk.ArtifactInfoSubject
import com.birbit.artifactfinder.parser.testapk.SourceFile
import com.birbit.artifactfinder.parser.testapk.TestApk
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedMethodInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class CodeSourceParserTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private val rebuild = false
    @Test
    fun compilation() {
        @Suppress("ConstantConditionIf")
        val aarOutput = if (rebuild) {
            val outFolder = tmpFolder.newFolder()
            TestApk(
                // TODO test anonymous nested class
                // TODO test nested of inner
                sources = listOf(
                    SRC_USER, SRC_INNER_CLASS, SRC_INVISIBLE_OUTER_CLASS,
                    SRC_KOTLIN_INNER_CLASS, SRC_KOTLIN_INVISIBLE_OUTER_CLASS,
                    SRC_LOWERCASE, SRC_RESTRICTED, SRC_KOTLIN_RESTRICTED
                ),
                tmpFolder = outFolder
            ).buildAar().also {
                val resourceAar = File("src/test/resources/lib-release.aar")
                if(resourceAar.exists()) {
                    // copy into resources
                    outFolder.resolve("lib/build/outputs/aar/lib-release.aar").also {
                        check(it.exists())
                    }.copyTo(resourceAar, overwrite = true)
                    println("copied output to resources")
                } else {
                    println("no resources to copy to in ${resourceAar.absoluteFile.absolutePath}")
                }
            }
        } else {
            Aar(
                CodeSourceParserTest::class.java.getResourceAsStream("/lib-release.aar")
            )
        }
        val artifactInfo = CodeSourceParser.parse(aarOutput)
        ArtifactInfoSubject.assertThat(artifactInfo).hasExactClasses(
            listOf(
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "User"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "AnotherClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "OuterClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "OuterClass\$InnerClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "OuterClass\$InnerClass\$DoubleInnerClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass\$KotlinInnerClass"
                ),
                ParsedClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass\$KotlinInnerClass\$KotlinDoubleInnerClass"
                )
            )
        ).hasExactGlobalMethods(
            listOf(
                ParsedMethodInfo(
                    receiver = null,
                    name = "globalMethod",
                    pkg = "com.test"
                )
            )
        ).hasExactExtensionMethods(
            listOf(
                ParsedMethodInfo(
                    receiver = ParsedClassInfo(
                        pkg = "kotlin",
                        name = "String"
                    ),
                    name = "myMethod",
                    pkg = "com.test"
                )
            )
        )
    }

    companion object {
        val SRC_USER = SourceFile(
            path = "com/test/User.kt",
            code = """
                package com.test
                data class User(val age:Int)
                class AnotherClass {
                    fun anotherClassMethod() = 3
                    // this should not be included
                    fun String.classExt() = 3
                    companion object {
                        private fun companionMethod() {
                        }
                        // this should not be included
                        fun String.companionExt() = 3
                    }
                }
                fun String.myMethod(myArg:Int) : Long = 3L
                fun globalMethod(someArg : String) : String = "foo"
                internal fun String.internalMyMethod(myArg:Int) : Long = 3L
                internal fun internalGlobalMethod(someArg : String) : String = "foo"
                
                internal fun internalFun():Unit {}
                internal class InternalClass(val x : Int)
                class kotlinLowercase()
            """.trimIndent()
        )
        val SRC_LOWERCASE = SourceFile(
            path = "com/test/lowercase.java",
            code = """
                package com.test;
                public class lowercase {
                }
            """.trimIndent()
        )

        val SRC_RESTRICTED = SourceFile(
            path = "com/test/MyRestricted.java",
            code = """
                package com.test;
                import androidx.annotation.RestrictTo;
                @RestrictTo(RestrictTo.Scope.LIBRARY)
                public class MyRestricted {
                
                }
            """.trimIndent()
        )

        val SRC_INNER_CLASS = SourceFile(
            path = "com/test/OuterClass.java",
            code = """
                package com.test;
                public class OuterClass {
                    public static class InnerClass {
                        public static class DoubleInnerClass {
                            public void doSomething() {
                            }
                        }
                    }
                }
            """.trimIndent()
        )

        val SRC_INVISIBLE_OUTER_CLASS = SourceFile(
            path = "com/test/InvisibleOuterClass.java",
            code = """
                package com.test;
                class InvisibleOuterClass {
                    public static class InvisibleInnerClass {
                        public static class InvisibleDoubleInnerClass {
                        }
                    }
                }
            """.trimIndent()
        )

        val SRC_KOTLIN_INNER_CLASS = SourceFile(
            path = "com/test/KotlinOuterClass.kt",
            code = """
                package com.test;
                class KotlinOuterClass {
                    class KotlinInnerClass {
                        class KotlinDoubleInnerClass {
                        }
                    }
                }
            """.trimIndent()
        )

        val SRC_KOTLIN_INVISIBLE_OUTER_CLASS = SourceFile(
            path = "com/test/KotlinInvisibleOuterClass.kt",
            code = """
                package com.test;
                internal class KotlinInvisibleOuterClass {
                    class KotlinInvisibleInnerClass {
                        class KotlinDoubleInvisibleInnerClass {
                        }
                    }
                }
            """.trimIndent()
        )

        val SRC_KOTLIN_RESTRICTED = SourceFile(
            path = "com/test/MyRestrictedKotlin.kt",
            code = """
                package com.test
                import androidx.annotation.RestrictTo
                
                @RestrictTo(RestrictTo.Scope.LIBRARY)
                public class MyRestrictedKotlin {
                
                }
            """.trimIndent()
        )
    }
}