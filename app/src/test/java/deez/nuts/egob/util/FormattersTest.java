package deez.nuts.egob.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FormattersTest {

    @Test
    public void initialsUseFirstTwoWords() {
        assertEquals("EV", Formatters.initials("Elena Vargas"));
        assertEquals("EV", Formatters.initials("  Elena   Vargas  "));
        assertEquals("EG", Formatters.initials(null));
        assertEquals("EG", Formatters.initials("   "));
    }

    @Test
    public void currencyUsesTwoDecimals() {
        assertEquals("$45.00", Formatters.currency(45));
        assertEquals("$0.00", Formatters.currency(0));
    }

    @Test
    public void relativeTimeReadsNaturally() {
        assertEquals("Ahora", Formatters.relative(System.currentTimeMillis()));
        assertTrue(Formatters.relative(System.currentTimeMillis() - 5 * 60 * 1000).startsWith("Hace 5"));
        assertFalse(Formatters.date(System.currentTimeMillis()).isEmpty());
    }
}
