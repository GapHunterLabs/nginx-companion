package dev.gaphunter.nginxcompanion.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Hand-rolled lexer for nginx config syntax: comments (`# ...`), quoted
 * strings, `$variables`, braces/semicolons, and everything else as a bare
 * WORD token (directive names and directive values are lexically
 * indistinguishable without knowing the directive, so both come through
 * as WORD — the completion contributor and highlighter decide what to do
 * with a given word from context/lookup, not the lexer).
 *
 * No external grammar library: nginx's syntax is small and stable enough
 * that a manual scanner is simpler than wiring up Grammar-Kit for it.
 */
class NginxLexer : LexerBase() {

    private lateinit var buffer: CharSequence
    private var startOffset = 0
    private var endOffset = 0

    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        locateToken()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        locateToken()
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun locateToken() {
        if (tokenStart >= endOffset) {
            tokenType = null
            tokenEnd = tokenStart
            return
        }
        val c = buffer[tokenStart]
        when {
            c.isWhitespace() -> {
                tokenType = NginxTokenTypes.WHITESPACE
                tokenEnd = scanWhile(tokenStart) { it.isWhitespace() }
            }
            c == '#' -> {
                tokenType = NginxTokenTypes.COMMENT
                tokenEnd = scanWhile(tokenStart + 1) { it != '\n' }
            }
            c == '"' || c == '\'' -> {
                tokenType = NginxTokenTypes.STRING
                tokenEnd = scanQuotedString(tokenStart, c)
            }
            c == '$' -> {
                tokenType = NginxTokenTypes.VARIABLE
                tokenEnd = scanVariable(tokenStart)
            }
            c == '{' -> {
                tokenType = NginxTokenTypes.LBRACE
                tokenEnd = tokenStart + 1
            }
            c == '}' -> {
                tokenType = NginxTokenTypes.RBRACE
                tokenEnd = tokenStart + 1
            }
            c == ';' -> {
                tokenType = NginxTokenTypes.SEMICOLON
                tokenEnd = tokenStart + 1
            }
            else -> {
                tokenType = NginxTokenTypes.WORD
                tokenEnd = scanWhile(tokenStart) { !isSpecial(it) }
            }
        }
        if (tokenEnd <= tokenStart) {
            // Safety net: never emit a zero-length token (would infinite-loop
            // the platform's lexer-consistency checks).
            tokenType = NginxTokenTypes.BAD_CHARACTER
            tokenEnd = tokenStart + 1
        }
    }

    private fun isSpecial(c: Char): Boolean =
        c.isWhitespace() || c == '{' || c == '}' || c == ';' || c == '"' || c == '\'' || c == '$' || c == '#'

    private fun scanWhile(from: Int, predicate: (Char) -> Boolean): Int {
        var i = from
        while (i < endOffset && predicate(buffer[i])) i++
        return i
    }

    private fun scanQuotedString(from: Int, quote: Char): Int {
        var i = from + 1
        while (i < endOffset) {
            val c = buffer[i]
            if (c == '\\' && i + 1 < endOffset) {
                i += 2
                continue
            }
            if (c == quote) {
                return i + 1
            }
            if (c == '\n') {
                // Unterminated string on this line: stop before the newline
                // rather than swallowing the rest of the file.
                return i
            }
            i++
        }
        return i
    }

    private fun scanVariable(from: Int): Int {
        var i = from + 1
        if (i < endOffset && buffer[i] == '{') {
            i++
            while (i < endOffset && buffer[i] != '}') i++
            return if (i < endOffset) i + 1 else i
        }
        while (i < endOffset && (buffer[i].isLetterOrDigit() || buffer[i] == '_')) i++
        return if (i > from + 1) i else from + 1
    }
}
