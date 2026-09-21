package cz.vitskalicky.lepsirozvrh

import cz.vitskalicky.lepsirozvrh.update.UpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `a later patch release is newer`() {
        assertTrue(UpdateChecker.isNewer("v2.0.29", "2.0.28"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(UpdateChecker.isNewer("v2.0.28", "2.0.28"))
    }

    @Test
    fun `an older release is not newer`() {
        assertFalse(UpdateChecker.isNewer("v2.0.27", "2.0.28"))
    }

    /** The whole reason this is not a string comparison: "2.0.9" < "2.0.28" as text. */
    @Test
    fun `two-digit patch beats one-digit patch`() {
        assertTrue(UpdateChecker.isNewer("v2.0.28", "2.0.9"))
        assertFalse(UpdateChecker.isNewer("v2.0.9", "2.0.28"))
    }

    @Test
    fun `minor and major bumps win over patch`() {
        assertTrue(UpdateChecker.isNewer("v2.1.0", "2.0.99"))
        assertTrue(UpdateChecker.isNewer("v3.0.0", "2.99.99"))
    }

    /** Debug builds carry a versionName suffix; it must not read as a newer version. */
    @Test
    fun `debug suffix on the running version is ignored`() {
        assertFalse(UpdateChecker.isNewer("v2.0.28", "2.0.28.debug_abc1234"))
        assertTrue(UpdateChecker.isNewer("v2.0.29", "2.0.28.debug_abc1234"))
    }

    @Test
    fun `missing segments count as zero`() {
        assertTrue(UpdateChecker.isNewer("v2.1", "2.0.28"))
        assertFalse(UpdateChecker.isNewer("v2.0", "2.0.0"))
    }
}
