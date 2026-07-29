package dev.gaphunter.nginxcompanion.detection

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.io.ByteSequence
import dev.gaphunter.nginxcompanion.lang.NginxFileType
import java.nio.charset.StandardCharsets

/**
 * Only overrides files that already look like nginx config by content
 * (see [NginxConfigDetector]) — never claims every `.conf` file the way
 * the incumbent's forced takeover was reported to. An opt-in heuristic,
 * not an extension claim.
 */
class NginxFileTypeOverrider : FileTypeRegistry.FileTypeDetector {

    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        val sample = firstCharsIfText?.toString() ?: String(firstBytes.toBytes(), StandardCharsets.UTF_8)
        return if (NginxConfigDetector.isNginxConfig(file.name, sample)) NginxFileType else null
    }

    @Deprecated("Overrides a deprecated platform member; still the correct hook for cache invalidation.")
    override fun getVersion(): Int = 2
}
