package dev.gaphunter.nginxcompanion.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import dev.gaphunter.nginxcompanion.directives.NginxDirectiveIndex
import dev.gaphunter.nginxcompanion.lang.NginxLanguage
import dev.gaphunter.nginxcompanion.lang.NginxTokenTypes

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
                        )
                    }
                }
            },
        )
    }
}
