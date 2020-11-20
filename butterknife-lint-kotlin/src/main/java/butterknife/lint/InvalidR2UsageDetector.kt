package butterknife.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.LintUtils.skipParentheses
import com.android.tools.lint.detector.api.Scope.Companion.JAVA_FILE_SCOPE
import com.google.common.collect.ImmutableSet
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Custom lint rule to make sure that generated R2 is not referenced outside annotations.
 */
class InvalidR2UsageDetector : Detector(), UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                node.accept(R2UsageVisitor(context))
            }
        }
    }

    private class R2UsageVisitor internal constructor(private val context: JavaContext) : AbstractUastVisitor() {
        override fun visitAnnotation(annotation: UAnnotation): Boolean {
            // skip annotations
            return true
        }

        override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression): Boolean {
            detectR2(context, node)
            return super.visitQualifiedReferenceExpression(node)
        }

        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
            detectR2(context, node)
            return super.visitSimpleNameReferenceExpression(node)
        }

        companion object {
            private fun detectR2(context: JavaContext, node: UElement) {
                val sourceFile = context.uastFile
                val classes = sourceFile!!.classes
                if (!classes.isEmpty() && classes[0].name != null) {
                    val qualifiedName = classes[0].name!!
                    if (qualifiedName.contains("_ViewBinder")
                            || qualifiedName.contains("_ViewBinding")
                            || qualifiedName == R2) {
                        // skip generated files and R2
                        return
                    }
                }
                val isR2 = isR2Expression(node)
                if (isR2 && !context.isSuppressedWithComment(node, ISSUE)) {
                    context.report(ISSUE, node, context.getLocation(node), LINT_ERROR_BODY)
                }
            }

            private fun isR2Expression(node: UElement): Boolean {
                val parentNode = node.uastParent ?: return false
                val text = node.asSourceString()
                val parent = skipParentheses(parentNode)
                return ((text == R2 || text.contains(".R2"))
                        && parent is UExpression
                        && endsWithAny(parent.asSourceString(), SUPPORTED_TYPES))
            }

            private fun endsWithAny(text: String, possibleValues: Set<String>): Boolean {
                val tokens = text.split("\\.").toTypedArray()
                return tokens.size > 1 && possibleValues.contains(tokens[tokens.size - 1])
            }
        }

    }

    companion object {
        private const val LINT_ERROR_BODY = "R2 should only be used inside annotations"
        private const val LINT_ERROR_TITLE = "Invalid usage of R2"
        private const val ISSUE_ID = "InvalidR2Usage"
        private val SUPPORTED_TYPES: Set<String> = ImmutableSet.of("array", "attr", "bool", "color", "dimen", "drawable", "id", "integer", "string")
        val ISSUE: Issue = Issue.create(ISSUE_ID, LINT_ERROR_TITLE, LINT_ERROR_BODY, CORRECTNESS, 6, Severity.ERROR, Implementation(InvalidR2UsageDetector::class.java, JAVA_FILE_SCOPE))
        private const val R2 = "R2"
    }
}