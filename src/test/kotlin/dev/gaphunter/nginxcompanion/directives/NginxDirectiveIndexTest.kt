package dev.gaphunter.nginxcompanion.directives

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NginxDirectiveIndexTest {

    @Test
    fun loadsARealisticNumberOfDirectives() {
        // The real bundled catalog has 940 entries across 94 modules; a
        // loose lower bound guards against the resource silently going
        // missing/empty without hardcoding the exact count.
        assertTrue(NginxDirectiveIndex.directives.size > 500)
    }

    @Test
    fun wellKnownCoreDirectivesArePresent() {
        for (directive in listOf("listen", "server_name", "location", "proxy_pass", "worker_processes")) {
            assertTrue("expected '$directive' in the catalog", NginxDirectiveIndex.isKnownDirective(directive))
        }
    }

    @Test
    fun arbitraryWordsAreNotKnownDirectives() {
        assertFalse(NginxDirectiveIndex.isKnownDirective("this_is_not_a_directive"))
        assertFalse(NginxDirectiveIndex.isKnownDirective(""))
    }

    @Test
    fun eachDirectiveHasANonEmptyModule() {
        for (directive in NginxDirectiveIndex.directives) {
            assertTrue(directive.name.isNotBlank())
            assertTrue(directive.module.isNotBlank())
        }
    }

    @Test
    fun namesSetSizeMatchesUniqueDirectiveNames() {
        assertEquals(NginxDirectiveIndex.directives.map { it.name }.toSet().size, NginxDirectiveIndex.names.size)
    }
}
