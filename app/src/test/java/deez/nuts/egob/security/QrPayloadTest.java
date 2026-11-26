package deez.nuts.egob.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class QrPayloadTest {

    private static final String SECRET = "clave-de-prueba-egob";

    @Test
    public void signedPayloadVerifies() {
        long now = System.currentTimeMillis();
        String payload = QrPayload.create("NAT-8829-4102-X", "Elena Vargas", now, SECRET);

        assertTrue(QrPayload.verify(payload, SECRET));
        assertEquals("NAT-8829-4102-X", QrPayload.extractNin(payload));
        assertEquals(now, QrPayload.extractTimestamp(payload));
    }

    @Test
    public void tamperedPayloadIsRejected() {
        long now = System.currentTimeMillis();
        String payload = QrPayload.create("NAT-8829-4102-X", "Elena Vargas", now, SECRET);
        String tampered = payload.replace("Elena Vargas", "Otra Persona");

        assertFalse(QrPayload.verify(tampered, SECRET));
        assertFalse(QrPayload.verify(payload, "otra-clave"));
        assertFalse(QrPayload.verify("EGOB1|invalido", SECRET));
    }
}
