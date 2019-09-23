package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.testapk.ArtifactInfoSubject
import com.birbit.artifactfinder.parser.testapk.SourceFile
import com.birbit.artifactfinder.parser.testapk.TestApk
import com.birbit.artifactfinder.parser.vo.ParsedClassInfo
import com.birbit.artifactfinder.parser.vo.ParsedExtensionMethodInfo
import com.birbit.artifactfinder.parser.vo.ParsedGlobalMethodInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CodeSourceParserTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    val rebuild = true
    @Test
    fun compilation() {
        @Suppress("ConstantConditionIf")
        val aarOutput = if (rebuild) {
            TestApk(
                // TODO test anonymous nested class
                // TODO test nested of inner
                sources = listOf(
                    SRC_USER, SRC_INNER_CLASS, SRC_INVISIBLE_OUTER_CLASS,
                    SRC_KOTLIN_INNER_CLASS, SRC_KOTLIN_INVISIBLE_OUTER_CLASS,
                    SRC_LOWERCASE
                ),
                tmpFolder = tmpFolder.newFolder()
            ).buildAar()
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
                ParsedGlobalMethodInfo("globalMethod")
            )
        ).hasExactExtensionMethods(
            listOf(
                ParsedExtensionMethodInfo(
                    receiver = ParsedClassInfo(
                        pkg = "kotlin",
                        name = "String"
                    ),
                    name = "myMethod"
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
                }
                fun String.myMethod(myArg:Int) : Long = 3L
                fun globalMethod(someArg : String) : String = "foo"
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

    }
}