package dev.gaphunter.nginxcompanion.lang

import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Test

class NginxLexerTest {

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = NginxLexer()
        lexer.start(text, 0, text.length, 0)
        val result = mutableListOf<Pair<IElementType, String>>()
        while (true) {
            val type = lexer.tokenType ?: break
            result.add(type to text.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    private fun nonWhitespace(text: String) = tokenize(text).filter { it.first != NginxTokenTypes.WHITESPACE }

    @Test
    fun bracesAndSemicolon() {
        val tokens = nonWhitespace("events { worker_connections 768; }")
        assertEquals(
            listOf(
                NginxTokenTypes.WORD to "events",
                NginxTokenTypes.LBRACE to "{",
                NginxTokenTypes.WORD to "worker_connections",
                NginxTokenTypes.WORD to "768",
                NginxTokenTypes.SEMICOLON to ";",
                NginxTokenTypes.RBRACE to "}",
            ),
            tokens,
        )
    }

    @Test
    fun commentRunsToEndOfLineOnly() {
        val tokens = nonWhitespace("# a comment\nlisten 80;")
        assertEquals(NginxTokenTypes.COMMENT, tokens[0].first)
        assertEquals("# a comment", tokens[0].second)
        assertEquals(NginxTokenTypes.WORD to "listen", tokens[1])
    }

    @Test
    fun doubleQuotedStringWithEscapedQuote() {
        val tokens = nonWhitespace("""add_header X-Test "a \"quoted\" value";""")
        val string = tokens.first { it.first == NginxTokenTypes.STRING }
        assertEquals(""""a \"quoted\" value"""", string.second)
    }

    @Test
    fun singleQuotedString() {
        val tokens = nonWhitespace("log_format main 'combined';")
        assertEquals(NginxTokenTypes.STRING to "'combined'", tokens.last { it.first == NginxTokenTypes.STRING })
    }

    @Test
    fun simpleVariable() {
        val tokens = nonWhitespace("proxy_set_header Host \$host;")
        assertEquals(NginxTokenTypes.VARIABLE to "\$host", tokens[2])
    }

    @Test
    fun bracedVariable() {
        val tokens = nonWhitespace("return 301 \${scheme}://example.com;")
        val variable = tokens.first { it.first == NginxTokenTypes.VARIABLE }
        assertEquals("\${scheme}", variable.second)
    }

    @Test
    fun regexLocationDoesNotBreakLexing() {
        val tokens = nonWhitespace("""location ~* \.(jpg|png)$ { }""")
        assertEquals(NginxTokenTypes.WORD to "location", tokens.first())
        assertEquals(NginxTokenTypes.LBRACE, tokens[tokens.size - 2].first)
        assertEquals(NginxTokenTypes.RBRACE, tokens.last().first)
    }

    @Test
    fun emptyInputProducesNoTokens() {
        assertEquals(emptyList<Pair<IElementType, String>>(), tokenize(""))
    }

    @Test
    fun unterminatedStringStopsAtNewline() {
        val tokens = nonWhitespace("a \"unterminated\nb;")
        assertEquals(NginxTokenTypes.STRING to "\"unterminated", tokens[1])
        assertEquals(NginxTokenTypes.WORD to "b", tokens[2])
    }
}
