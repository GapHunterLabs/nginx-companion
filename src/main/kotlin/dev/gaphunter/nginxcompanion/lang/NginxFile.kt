package dev.gaphunter.nginxcompanion.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class NginxFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, NginxLanguage) {
    override fun getFileType() = NginxFileType
    override fun toString(): String = "Nginx Config File"
}
