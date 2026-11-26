package deez.nuts.egob.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RequestStatusTest {

    @Test
    public void pendingCanMoveToActionOrCompletion() {
        assertTrue(RequestStatus.canTransition(RequestStatus.PENDING, RequestStatus.ACTION));
        assertTrue(RequestStatus.canTransition(RequestStatus.PENDING, RequestStatus.IN_PROGRESS));
        assertTrue(RequestStatus.canTransition(RequestStatus.PENDING, RequestStatus.COMPLETED));
        assertTrue(RequestStatus.canTransition(RequestStatus.PENDING, RequestStatus.REJECTED));
    }

    @Test
    public void completedAndRejectedAreTerminal() {
        assertFalse(RequestStatus.canTransition(RequestStatus.COMPLETED, RequestStatus.PENDING));
        assertFalse(RequestStatus.canTransition(RequestStatus.REJECTED, RequestStatus.ACTION));
        assertFalse(RequestStatus.canTransition(RequestStatus.PENDING, RequestStatus.PENDING));
    }

    @Test
    public void actionAndInProgressCanResolve() {
        assertTrue(RequestStatus.canTransition(RequestStatus.ACTION, RequestStatus.IN_PROGRESS));
        assertTrue(RequestStatus.canTransition(RequestStatus.ACTION, RequestStatus.COMPLETED));
        assertTrue(RequestStatus.canTransition(RequestStatus.IN_PROGRESS, RequestStatus.ACTION));
        assertTrue(RequestStatus.canTransition(RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED));
    }

    @Test
    public void openStatusesAreReportedInSpanish() {
        assertTrue(RequestStatus.isOpen(RequestStatus.PENDING));
        assertTrue(RequestStatus.isOpen(RequestStatus.ACTION));
        assertTrue(RequestStatus.isOpen(RequestStatus.IN_PROGRESS));
        assertFalse(RequestStatus.isOpen(RequestStatus.COMPLETED));
        assertFalse(RequestStatus.isOpen(RequestStatus.REJECTED));
        assertEquals("Pendiente", RequestStatus.label(RequestStatus.PENDING));
        assertEquals("Acción requerida", RequestStatus.label(RequestStatus.ACTION));
        assertEquals("En proceso", RequestStatus.label(RequestStatus.IN_PROGRESS));
        assertEquals("Completada", RequestStatus.label(RequestStatus.COMPLETED));
        assertEquals("Rechazada", RequestStatus.label(RequestStatus.REJECTED));
    }
}
