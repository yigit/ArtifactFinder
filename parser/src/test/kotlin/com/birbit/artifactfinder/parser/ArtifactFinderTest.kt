package com.birbit.artifactfinder.parser

import com.birbit.artifactfinder.parser.testapk.ArtifactInfoSubject
import com.birbit.artifactfinder.parser.testapk.SourceFile
import com.birbit.artifactfinder.parser.testapk.TestApk
import com.birbit.artifactfinder.parser.vo.ClassInfo
import com.birbit.artifactfinder.parser.vo.ExtensionMethodInfo
import com.birbit.artifactfinder.parser.vo.GlobalMethodInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ArtifactFinderTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    val rebuild = false
    @Test
    fun compilation() {
        val aarOutput = if (rebuild) {
            TestApk(
                // TODO test anonymous nested class
                // TODO test nested of inner
                sources = listOf(
                    SRC_USER, SRC_INNER_CLASS, SRC_INVISIBLE_OUTER_CLASS,
                    SRC_KOTLIN_INNER_CLASS, SRC_KOTLIN_INVISIBLE_OUTER_CLASS,
                    SRC_LOWERCASE
                ),
                tmpFolder = File("/home/yboyar/ArtifactFinderTest")//tmpFolder.newFolder()
            ).buildAar()
        } else {
            Aar(
                ArtifactFinderTest::class.java.getResourceAsStream("/lib-release.aar")
            )
        }
        val artifactInfo = CodeSourceParser.parse(aarOutput)
        ArtifactInfoSubject.assertThat(artifactInfo).hasExactClasses(
            listOf(
                ClassInfo(
                    pkg = "com.test",
                    name = "User"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "AnotherClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "OuterClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "OuterClass\$InnerClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "OuterClass\$InnerClass\$DoubleInnerClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass\$KotlinInnerClass"
                ),
                ClassInfo(
                    pkg = "com.test",
                    name = "KotlinOuterClass\$KotlinInnerClass\$KotlinDoubleInnerClass"
                )
            )
        ).hasExactGlobalMethods(
            listOf(
                GlobalMethodInfo("globalMethod")
            )
        ).hasExactExtensionMethods(
            listOf(
                ExtensionMethodInfo(
                    receiver = ClassInfo(
                        pkg = "kotlin",
                        name = "String"
                    ),
                    name = "myMethod"
                )
            )
        )

        artifactInfo.let {
            println(it)
        }
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