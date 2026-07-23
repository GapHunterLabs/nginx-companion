package dev.gaphunter.nginxcompanion.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import dev.gaphunter.nginxcompanion.directives.NginxDirectiveIndex
import dev.gaphunter.nginxcompanion.lang.NginxHighlighterColors
import dev.gaphunter.nginxcompanion.lang.NginxTokenTypes

/**
 * Colors a WORD token as a known directive only when its text is a real
 * nginx directive name from the bundled catalog. Directive names and
 * directive values are lexically identical WORD tokens (see NginxLexer),
 * so this lookup — not the lexer — is what tells them apart.
 */
class NginxDirectiveAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.node?.elementType != NginxTokenTypes.WORD) return
        if (!NginxDirectiveIndex.isKnownDirective(element.text)) return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(NginxHighlighterColors.KNOWN_DIRECTIVE)
            .range(element.textRange)
            .create()
    }
}
