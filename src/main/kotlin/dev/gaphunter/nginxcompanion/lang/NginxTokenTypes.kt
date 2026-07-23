package dev.gaphunter.nginxcompanion.lang

import com.intellij.psi.tree.IElementType

class NginxTokenType(debugName: String) : IElementType(debugName, NginxLanguage)

object NginxTokenTypes {
    val WHITESPACE = NginxTokenType("NGINX_WHITESPACE")
    val COMMENT = NginxTokenType("NGINX_COMMENT")
    val STRING = NginxTokenType("NGINX_STRING")
    val VARIABLE = NginxTokenType("NGINX_VARIABLE")
    val LBRACE = NginxTokenType("NGINX_LBRACE")
    val RBRACE = NginxTokenType("NGINX_RBRACE")
    val SEMICOLON = NginxTokenType("NGINX_SEMICOLON")
    val WORD = NginxTokenType("NGINX_WORD")
    val BAD_CHARACTER = NginxTokenType("NGINX_BAD_CHARACTER")
}
