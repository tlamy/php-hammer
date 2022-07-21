package net.rentalhost.plugins.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.php.lang.formatter.ui.predefinedStyle.PSR12CodeStyle
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.ConstantReference
import com.jetbrains.php.lang.psi.elements.If
import com.jetbrains.php.lang.psi.elements.Statement
import com.jetbrains.php.lang.psi.elements.impl.NewExpressionImpl

object FormatterService {
    private val projectCodeStyle: CodeStyleSettings = CodeStyleSettingsManager().createSettings()

    init {
        PSR12CodeStyle().apply(projectCodeStyle)
    }

    private object ElementReassemble {
        fun visit(element: PsiElement, appender: (String) -> Unit) {
            when {
                element is PsiComment ||
                element is PsiWhiteSpace -> return

                element is ConstantReference ||
                element is ClassReference ||
                element::class == LeafPsiElement::class -> appender.invoke(element.text.lowercase())

                element is NewExpressionImpl &&
                element.parameters.isEmpty() -> appender.invoke("new " + (element.classReference ?: return).text.lowercase())

                else -> {
                    var child = element.firstChild

                    if (child == null) {
                        appender.invoke(element.text)
                        return
                    }

                    while (child != null) {
                        visit(child, appender)
                        child = child.nextSibling
                    }
                }
            }
        }
    }

    fun normalizeText(element: PsiElement, postfix: String = "|"): String {
        var elementText = ""

        ElementReassemble.visit(element) { s -> elementText += s + postfix }

        return elementText
    }

    fun normalize(project: Project, element: PsiElement): Statement =
        (PhpPsiElementFactory.createStatement(project, "if(1)${normalizeText(element, "")}") as If).statement!!
}
