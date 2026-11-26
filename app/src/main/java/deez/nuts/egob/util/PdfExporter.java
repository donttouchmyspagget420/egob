package deez.nuts.egob.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import deez.nuts.egob.database.entities.Attachment;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.pojos.RequestWithService;

/** Creates simple, real PDF receipts and identity extracts without external libraries. */
public final class PdfExporter {

    private static final int PAGE_WIDTH = 595;   // A4 at 72 dpi
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 48;

    private PdfExporter() {
    }

    public static File exportProfile(Context context, Profile profile) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(info);
        Canvas canvas = page.getCanvas();

        Paint title = paint(22, true, Color.rgb(0, 15, 37));
        Paint heading = paint(12, true, Color.rgb(0, 103, 129));
        Paint body = paint(11, false, Color.rgb(13, 28, 47));
        Paint small = paint(9, false, Color.rgb(68, 71, 77));

        int y = MARGIN;
        canvas.drawText("eGob — Extracto de identidad", MARGIN, y, title);
        y += 12;
        canvas.drawText("Documento oficial de demostración · Emitido " + Formatters.date(System.currentTimeMillis()),
                MARGIN, y, small);
        y += 28;

        y = section(canvas, "DATOS DEL CIUDADANO", heading, y);
        y = row(canvas, "Nombre completo", safe(profile.fullName), body, y);
        y = row(canvas, "NIN", safe(profile.dni), body, y);
        y = row(canvas, "Fecha de nacimiento", safe(profile.dateOfBirth), body, y);
        y = row(canvas, "Domicilio", safe(profile.adress), body, y);
        y = row(canvas, "Ciudad / código postal", safe(profile.city) + " " + safe(profile.code), body, y);
        y = row(canvas, "Distrito", safe(profile.district), body, y);
        y = row(canvas, "Estado civil", safe(profile.maritalStatus), body, y);
        y = row(canvas, "Colegio electoral", safe(profile.pollingPlace), body, y);
        y += 16;

        y = section(canvas, "ESTADO DE LA CREDENCIAL", heading, y);
        y = row(canvas, "Nivel", safe(profile.tier), body, y);
        y = row(canvas, "Miembro desde", safe(profile.memberSince), body, y);
        y = row(canvas, "Verificación", profile.verified ? "Verificado" : "Pendiente", body, y);
        y = row(canvas, "Biometría", profile.biometricEnabled ? "Activa" : "Inactiva", body, y);
        y = row(canvas, "Doble factor", profile.twoFactorEnabled ? "Configurado" : "Pendiente", body, y);
        y += 24;

        canvas.drawText("Huella de verificación: " + Integer.toHexString(profile.hashCode()).toUpperCase(),
                MARGIN, y, small);
        y += 14;
        canvas.drawText("Este documento no sustituye a la credencial física ni al pasaporte.", MARGIN, y, small);

        document.finishPage(page);
        File output = new File(context.getCacheDir(), "egob_identidad_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            document.writeTo(stream);
        } finally {
            document.close();
        }
        return output;
    }

    public static File exportRequest(Context context, RequestWithService row, List<Attachment> attachments)
            throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(info);
        Canvas canvas = page.getCanvas();

        Paint title = paint(22, true, Color.rgb(0, 15, 37));
        Paint heading = paint(12, true, Color.rgb(0, 103, 129));
        Paint body = paint(11, false, Color.rgb(13, 28, 47));
        Paint small = paint(9, false, Color.rgb(68, 71, 77));

        int y = MARGIN;
        canvas.drawText("eGob — Comprobante de solicitud", MARGIN, y, title);
        y += 12;
        canvas.drawText("Comprobante emitido " + Formatters.dateTime(System.currentTimeMillis()),
                MARGIN, y, small);
        y += 28;

        y = section(canvas, "SOLICITUD", heading, y);
        y = row(canvas, "Referencia", safe(row.request.reference), body, y);
        y = row(canvas, "Servicio", safe(row.serviceName), body, y);
        y = row(canvas, "Autoridad", safe(row.serviceAuthority), body, y);
        y = row(canvas, "Estado", RequestStatus.label(row.request.status), body, y);
        y = row(canvas, "Fecha", Formatters.dateTime(row.request.createdAt), body, y);
        y = row(canvas, "Entrega", safe(row.request.deliveryMethod), body, y);
        y = row(canvas, "Tarifa", Formatters.currency(row.request.fee), body, y);
        y = row(canvas, "Oficial asignado", safe(row.request.assignedOfficer), body, y);
        y += 16;

        y = section(canvas, "DOCUMENTOS ADJUNTOS", heading, y);
        if (attachments == null || attachments.isEmpty()) {
            canvas.drawText("Sin documentos adjuntos.", MARGIN, y, body);
            y += 16;
        } else {
            for (Attachment attachment : attachments) {
                canvas.drawText("• " + attachment.name + " (" + FileStorage.humanSize(attachment.fileSize) + ")",
                        MARGIN, y, body);
                y += 16;
            }
        }

        y += 16;
        canvas.drawText("Conserva este comprobante. El estado puede verificarse en la pestaña Solicitudes.",
                MARGIN, y, small);

        document.finishPage(page);
        File output = new File(context.getCacheDir(),
                "egob_solicitud_" + row.request.reference + ".pdf");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            document.writeTo(stream);
        } finally {
            document.close();
        }
        return output;
    }

    private static int section(Canvas canvas, String text, Paint paint, int y) {
        canvas.drawText(text, MARGIN, y, paint);
        return y + 22;
    }

    private static int row(Canvas canvas, String label, String value, Paint paint, int y) {
        canvas.drawText(label + ":", MARGIN, y, paint);
        canvas.drawText(value == null || value.isEmpty() ? "—" : value, MARGIN + 150, y, paint);
        return y + 20;
    }

    private static Paint paint(int size, boolean bold, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setFakeBoldText(bold);
        return paint;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
