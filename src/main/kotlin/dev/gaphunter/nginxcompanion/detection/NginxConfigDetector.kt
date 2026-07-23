package dev.gaphunter.nginxcompanion.detection

/**
 * Decides whether a file is really an nginx config, from filename + a
 * content sample. Deliberately opt-in and conservative: `.conf` is used by
 * Apache, Redis, systemd units, and countless other tools, so matching on
 * extension alone would be exactly the "hijacks every YAML/CSV file"
 * complaint this whole plugin family exists to avoid.
 */
object NginxConfigDetector {

    private val EXACT_FILENAMES = setOf(
        "nginx.conf", "mime.types", "fastcgi_params", "fastcgi.conf",
        "uwsgi_params", "scgi_params", "koi-win", "koi-utf", "win-utf",
    )

    private val NGINX_DIRECTIVE_HINTS = listOf(
        "server_name", "location ", "location{", "proxy_pass", "fastcgi_pass",
        "upstream ", "events {", "events{", "http {", "http{", "listen ",
        "worker_processes", "root ", "try_files", "return ", "rewrite ",
        "add_header", "error_page", "access_log", "ssl_certificate",
    )

    private val APACHE_MARKERS = listOf(
        "<virtualhost", "<directory", "<location>", "</directory>",
        "servername ", "serverroot", "documentroot", "<ifmodule",
    )

    fun isNginxConfig(fileName: String, contentSample: String): Boolean {
        if (fileName.lowercase() in EXACT_FILENAMES) return true

        val lower = contentSample.lowercase()
        if (APACHE_MARKERS.any { lower.contains(it) }) return false

        val directiveHits = NGINX_DIRECTIVE_HINTS.count { lower.contains(it) }
        val looksBraceStatementShaped = lower.contains('{') && lower.contains(';')

        // Two independent nginx-specific directives is a much stronger
        // signal than one (avoids false-positiving on, say, a JSON file
        // that happens to mention "listen" once).
        return looksBraceStatementShaped && directiveHits >= 2
    }
}
