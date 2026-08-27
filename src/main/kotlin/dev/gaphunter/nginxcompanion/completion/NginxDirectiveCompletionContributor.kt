package dev.gaphunter.nginxcompanion.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import dev.gaphunter.nginxcompanion.directives.NginxDirectiveIndex
import dev.gaphunter.nginxcompanion.lang.NginxLanguage
import dev.gaphunter.nginxcompanion.lang.NginxTokenTypes
import dev.gaphunter.nginxcompanion.review.ReviewPrompt

class NginxDirectiveCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(NginxTokenTypes.WORD).withLanguage(NginxLanguage),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    for (directive in NginxDirectiveIndex.directives) {
                        result.addElement(
                            LookupElementBuilder.create(directive.name)
                                .withTypeText(directive.module.removePrefix("ngx_").removeSuffix("_module"))
                                // Real acceptance signal, not just the suggestion
                                // being shown in the popup -- InsertHandler only
                                // fires when the user actually picks this entry.
                                .withInsertHandler(RecordAcceptedDirective(directive.name))
                        )
                    }
                }
            },
        )
    }
}

/** Counts one real, accepted completion toward the review CTA -- never a suggestion merely shown in the popup. */
private class RecordAcceptedDirective(private val directiveName: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val path = context.file.virtualFile?.path ?: context.file.name
        ReviewPrompt.recordHit(context.project, "$path:${context.startOffset}:$directiveName")
    }
}
