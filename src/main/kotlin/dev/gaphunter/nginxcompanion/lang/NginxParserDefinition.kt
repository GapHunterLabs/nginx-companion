package dev.gaphunter.nginxcompanion.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Deliberately flat: every token becomes a direct leaf of the file root,
 * no nested statement/block nodes. Full nginx grammar (matching `{`/`}`
 * pairs, distinguishing directive name from arguments) isn't needed for
 * v0.1's feature set (lexer-based highlighting + a directive-name
 * completion list) and would be substantially more code for no user-
 * visible benefit yet.
 *
 * `createElement` still needs a real implementation, not a thrown
 * exception: the platform calls it while building the AST/PSI tree
 * regardless of grammar shape, and a throwing implementation silently
 * aborted PSI construction for this file type entirely (no highlighting,
 * no lexer/parser/highlighter invocations at all — see the project's
 * KNOWN_ISSUES.md for the full diagnosis trail). `ASTWrapperPsiElement`
 * is the platform's own generic wrapper for exactly this case (no
 * custom composite element hierarchy).
 */
class NginxParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(NginxLanguage)
    }

    override fun createLexer(project: Project): Lexer = NginxLexer()

    override fun createParser(project: Project): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.create(NginxTokenTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(NginxTokenTypes.STRING)

    override fun getWhitespaceTokens(): TokenSet =
        TokenSet.create(NginxTokenTypes.WHITESPACE, TokenType.WHITE_SPACE)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = NginxFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
}
