package dev.gaphunter.nginxcompanion.lang

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Explicit editor highlighter wiring for Nginx Config files.
 *
 * `lang.syntaxHighlighterFactory` alone should be enough for a
 * `LanguageFileType` (the platform is documented to derive the editor
 * highlighter from it automatically), but in practice the automatic path
 * never invoked [NginxSyntaxHighlighterFactory] for files resolved
 * through our content-based [NginxFileTypeOverrider] — registering this
 * provider explicitly bypasses whatever in that automatic resolution was
 * short-circuiting.
 */
class NginxEditorHighlighterProvider : EditorHighlighterProvider {
    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        virtualFile: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter = LexerEditorHighlighter(NginxSyntaxHighlighter(), colors)
}
