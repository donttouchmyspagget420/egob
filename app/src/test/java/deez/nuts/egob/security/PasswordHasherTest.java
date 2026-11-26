package deez.nuts.egob.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PasswordHasherTest {

    @Test
    public void verifiesCorrectPassword() {
        String stored = PasswordHasher.hash("egob2026");
        assertTrue(PasswordHasher.verify("egob2026", stored));
        assertFalse(PasswordHasher.verify("incorrecta", stored));
        assertFalse(PasswordHasher.verify(null, stored));
        assertFalse(PasswordHasher.verify("egob2026", "formato-invalido"));
    }

    @Test
    public void saltsMakeHashesUnique() {
        String first = PasswordHasher.hash("misma-clave");
        String second = PasswordHasher.hash("misma-clave");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("misma-clave", first));
        assertTrue(PasswordHasher.verify("misma-clave", second));
    }
}
