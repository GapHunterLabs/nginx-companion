package dev.gaphunter.nginxcompanion.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import dev.gaphunter.nginxcompanion.detection.NginxConfigDetector
import java.nio.charset.StandardCharsets
import javax.swing.Icon

/**
 * Implements [FileTypeIdentifiableByVirtualFile] in addition to being a
 * plain [LanguageFileType] -- see KNOWN_ISSUES.md Round 5 for the full
 * diagnosis trail. THIS ALONE IS NOT ENOUGH: the bundled TextMate plugin's
 * own `TextMateFileType` also implements `FileTypeIdentifiableByVirtualFile`
 * and competes in the exact same priority tier -- `FileTypeManagerImpl`
 * iterates its `specialFileTypes` array in *registration order* and
 * returns the first match, with no tie-breaking by specificity. TextMate,
 * being bundled, registers before third-party plugins, so it was winning
 * every time. `order="first"` on this class's `<fileType>` registration in
 * plugin.xml is what actually settles the race -- both pieces (this
 * interface AND that ordering attribute) are required together; either
 * one alone was confirmed insufficient via a live diagnostic listener
 * comparing what `FileTypeManager`/`PsiManager` actually resolved at the
 * exact moment a `.conf` tab opened.
 */
object NginxFileType : LanguageFileType(NginxLanguage), FileTypeIdentifiableByVirtualFile {
    override fun getName(): String = "Nginx Config"
    override fun getDescription(): String = "Nginx configuration file"
    override fun getDefaultExtension(): String = "conf"
    override fun getIcon(): Icon? = null

    /**
     * Cheap on purpose: only reads a bounded prefix of the file, same
     * content-sampling approach [dev.gaphunter.nginxcompanion.detection.NginxFileTypeOverrider]
     * already uses -- this method is called frequently by the platform
     * (per the interface's own JavaDoc), so no full-file read, no PSI
     * access, no indices.
     */
    override fun isMyFileType(file: VirtualFile): Boolean {
        val sampleBytes = try {
            file.inputStream.use { it.readNBytes(SAMPLE_BYTE_LIMIT) }
        } catch (e: Exception) {
            return false
        }
        val sample = String(sampleBytes, StandardCharsets.UTF_8)
        return NginxConfigDetector.isNginxConfig(file.name, sample)
    }

    private const val SAMPLE_BYTE_LIMIT = 8192
}
