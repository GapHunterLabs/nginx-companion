package dev.gaphunter.nginxcompanion.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NginxConfigDetectorTest {

    private val realNginxConf = """
        user www-data;
        worker_processes auto;

        events {
            worker_connections 768;
        }

        http {
            server {
                listen 80;
                server_name example.com;
                location / {
                    proxy_pass http://127.0.0.1:3000;
                }
            }
        }
    """.trimIndent()

    private val apacheConf = """
        <VirtualHost *:80>
            ServerName example.com
            DocumentRoot /var/www/html
            <Directory /var/www/html>
                AllowOverride All
            </Directory>
        </VirtualHost>
    """.trimIndent()

    private val redisConf = """
        port 6379
        bind 127.0.0.1
        save 900 1
        dir /var/lib/redis
    """.trimIndent()

    @Test
    fun exactFilenameMatchesRegardlessOfContent() {
        assertTrue(NginxConfigDetector.isNginxConfig("nginx.conf", ""))
        assertTrue(NginxConfigDetector.isNginxConfig("mime.types", "irrelevant"))
    }

    @Test
    fun exactFilenameMatchIsCaseInsensitive() {
        assertTrue(NginxConfigDetector.isNginxConfig("NGINX.CONF", ""))
    }

    @Test
    fun realNginxContentIsDetected() {
        assertTrue(NginxConfigDetector.isNginxConfig("sites-enabled/default", realNginxConf))
        assertTrue(NginxConfigDetector.isNginxConfig("app.conf", realNginxConf))
    }

    @Test
    fun apacheConfigIsRejectedEvenWithConfExtension() {
        assertFalse(NginxConfigDetector.isNginxConfig("httpd.conf", apacheConf))
    }

    @Test
    fun unrelatedConfFileIsRejected() {
        assertFalse(NginxConfigDetector.isNginxConfig("redis.conf", redisConf))
    }

    @Test
    fun singleWeakHintIsNotEnough() {
        // Only one nginx-ish hint and no brace/semicolon shape.
        assertFalse(NginxConfigDetector.isNginxConfig("notes.conf", "remember to check listen port"))
    }

    @Test
    fun emptyFileIsRejected() {
        assertFalse(NginxConfigDetector.isNginxConfig("random.conf", ""))
    }
}
