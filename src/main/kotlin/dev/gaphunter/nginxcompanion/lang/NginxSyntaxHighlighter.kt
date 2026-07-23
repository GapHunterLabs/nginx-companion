package dev.gaphunter.nginxcompanion.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

object NginxHighlighterColors {
    val COMMENT: TextAttributesKey =
        createTextAttributesKey("NGINX_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val STRING: TextAttributesKey =
        createTextAttributesKey("NGINX_STRING", DefaultLanguageHighlighterColors.STRING)
    val VARIABLE: TextAttributesKey =
        createTextAttributesKey("NGINX_VARIABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    val BRACES: TextAttributesKey =
        createTextAttributesKey("NGINX_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val SEMICOLON: TextAttributesKey =
        createTextAttributesKey("NGINX_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val KNOWN_DIRECTIVE: TextAttributesKey =
        createTextAttributesKey("NGINX_KNOWN_DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD)
    val BAD_CHARACTER: TextAttributesKey =
        createTextAttributesKey("NGINX_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}

class NginxSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = NginxLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when (tokenType) {
            NginxTokenTypes.COMMENT -> NginxHighlighterColors.COMMENT
            NginxTokenTypes.STRING -> NginxHighlighterColors.STRING
            NginxTokenTypes.VARIABLE -> NginxHighlighterColors.VARIABLE
            NginxTokenTypes.LBRACE, NginxTokenTypes.RBRACE -> NginxHighlighterColors.BRACES
            NginxTokenTypes.SEMICOLON -> NginxHighlighterColors.SEMICOLON
            NginxTokenTypes.BAD_CHARACTER -> NginxHighlighterColors.BAD_CHARACTER
            else -> return emptyArray()
        }
        return arrayOf(key)
    }
}

class NginxSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = NginxSyntaxHighlighter()
}
