package io.kotest.plugin.intellij

import com.intellij.psi.PsiElement

data class TestElement(
   val psi: PsiElement,
   val test: Test,
   val tests: List<TestElement>
)

data class TestName(
   val prefix: String?,
   val name: String,
   val focus: Boolean,
   val bang: Boolean,
   val interpolated: Boolean // set to true if the name contains one or more interpolated variables
) {
   companion object {
      operator fun invoke(prefix: String?, name: String, interpolated: Boolean): TestName {
         return when {
            name.trim().startsWith("!") -> TestName(prefix, name.trim().drop(1).trim(), focus = false, bang = true, interpolated = interpolated)
            name.trim().startsWith("f:") -> TestName(prefix, name.trim().drop(2).trim(), focus = true, bang = false, interpolated = interpolated)
            else -> TestName(prefix, name, focus = false, bang = false, interpolated = interpolated)
         }
      }
   }

   fun displayName(): String {
      val flattened = name.trim().replace("\n", "")
      return if (prefix == null) flattened else "$prefix$flattened"
   }
}

// components for the path, should not include prefixes
data class TestPathEntry(val name: String)

data class Test(
   val name: TestName, // the name as entered by the user
   val context: List<Test>, // components for the path, should not include prefixes
   val testType: TestType,
   val xdisabled: Boolean, // if true then this test was defined using one of the x methods
   val psi: PsiElement // the canonical element that identifies this test
) {

   // true if this test is not xdisabled and not disabled by a bang and not nested inside another disabled test
   val enabled: Boolean = !xdisabled && !name.bang && context.all { it.enabled }

   // true if this is a top level test (aka has no parents)
   val root = context.isEmpty()

   // true if this is not a top level test (aka is nested inside another test case)
   val isNested: Boolean = !root

   /**
    * Full path to this test is all parents plus this test
    */
   fun path() = context.map { TestPathEntry(it.name.name) } + TestPathEntry(name.name)

   /**
    * Returns the test path with delimiters so that the launcher can parse into components
    */
   fun testPath(): String = path().joinToString(" -- ") { it.name }

   /**
    * Returns the test path without delimiters for display to a user.
    */
   fun readableTestPath() = path().joinToString(" ") { it.name }
}

enum class TestType {
   Container, Test
}
