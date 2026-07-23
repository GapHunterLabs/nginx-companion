package dev.gaphunter.nginxcompanion.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object NginxFileType : LanguageFileType(NginxLanguage) {
    override fun getName(): String = "Nginx Config"
    override fun getDescription(): String = "Nginx configuration file"
    override fun getDefaultExtension(): String = "conf"
    override fun getIcon(): Icon? = null
}
