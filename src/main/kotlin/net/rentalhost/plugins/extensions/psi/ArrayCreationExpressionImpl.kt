package net.rentalhost.plugins.extensions.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl
import com.jetbrains.php.lang.psi.elements.impl.ArrayHashElementImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpPsiElementImpl
import net.rentalhost.plugins.services.ElementService
import net.rentalhost.plugins.services.FactoryService
import net.rentalhost.plugins.services.TypeService

fun ArrayCreationExpressionImpl.isVariadic(): Boolean =
    with(PsiTreeUtil.skipWhitespacesAndCommentsBackward(element)) {
        return this is LeafPsiElement && this.text == "..."
    }

fun ArrayCreationExpressionImpl.unpackValues(): MutableList<PsiElement> {
    val arrayElements = mutableListOf<PsiElement>()

    for (arrayElement in children) {
        if (arrayElement is PhpPsiElementImpl<*> &&
            TypeService.isVariadic(arrayElement, ArrayCreationExpressionImpl::class.java)) {
            arrayElements.addAll((arrayElement.firstPsiChild as ArrayCreationExpressionImpl).unpackValues())
        }
        else if (arrayElement is PhpPsiElementImpl<*> &&
                 TypeService.isVariadic(arrayElement)) {
            val compactNames = ElementService.getCompactNames(arrayElement.firstPsiChild as PsiElement)

            if (compactNames != null) {
                arrayElements.addAll(compactNames.map { FactoryService.createArrayKeyValue(arrayElement.project, "'$it'", "\$$it") })

                continue
            }

            arrayElements.add(arrayElement)
        }
        else if (arrayElement is ArrayHashElementImpl ||
                 arrayElement is PhpPsiElementImpl<*>) {
            arrayElements.add(arrayElement)
        }
    }

    return arrayElements
}
