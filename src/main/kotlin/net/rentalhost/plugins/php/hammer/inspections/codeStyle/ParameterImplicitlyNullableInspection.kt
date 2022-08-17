package net.rentalhost.plugins.php.hammer.inspections.codeStyle

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.refactoring.suggested.createSmartPointer
import com.jetbrains.php.config.PhpLanguageLevel
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.elements.Parameter
import com.jetbrains.php.lang.psi.elements.impl.ParameterImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpTypeDeclarationImpl
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import net.rentalhost.plugins.hammer.services.FactoryService
import net.rentalhost.plugins.php.hammer.services.ProblemsHolderService
import net.rentalhost.plugins.php.hammer.services.QuickFixService

class ParameterImplicitlyNullableInspection: PhpInspection() {
    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PhpElementVisitor = object: PhpElementVisitor() {
        override fun visitPhpParameter(element: Parameter) {
            if (element !is ParameterImpl ||
                !element.defaultValueType.isNullable)
                return

            val declaredType = element.declaredType

            if (declaredType.isEmpty ||
                declaredType.isNullable ||
                declaredType.types.contains(PhpType._MIXED))
                return

            val elementPointer = element.createSmartPointer()

            ProblemsHolderService.instance.registerProblem(
                problemsHolder,
                element,
                "parameter type is implicitly null",
                QuickFixService.instance.simpleInline("Add explicit \"null\" type") {
                    val elementLocal = elementPointer.element ?: return@simpleInline

                    if (elementLocal.typeDeclaration != null) {
                        (elementLocal.typeDeclaration ?: return@simpleInline)
                            .replace(FactoryService.createParameterType(problemsHolder.project, (elementLocal.typeDeclaration ?: return@simpleInline).text + "|null"))
                    }
                    else {
                        val parameterComplex = FactoryService.createComplexParameter(problemsHolder.project, elementLocal.text)

                        with(parameterComplex.typeDeclaration as PhpTypeDeclarationImpl) {
                            replace(FactoryService.createParameterType(problemsHolder.project, "$text|null"))
                        }

                        elementLocal.replace(FactoryService.createComplexParameterDoctypeCompatible(problemsHolder.project, parameterComplex.text))
                    }
                }
            )
        }
    }

    override fun getMinimumSupportedLanguageLevel(): PhpLanguageLevel = PhpLanguageLevel.PHP800
}
