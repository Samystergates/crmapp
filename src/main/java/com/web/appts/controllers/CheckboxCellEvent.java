package com.web.appts.controllers;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;

public class CheckboxCellEvent implements PdfPCellEvent {
    private boolean checked;

    public CheckboxCellEvent(boolean checked) {
        this.checked = checked;
    }

    @Override
    public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvas) {
        PdfContentByte cb = canvas[PdfPTable.LINECANVAS];

        // Draw the checkbox (rectangle)
        cb.rectangle(rect.getLeft() + 2, rect.getBottom() + 2, 10, 10);
        cb.stroke();

        // Draw the cross inside if checked
        if (checked) {
            cb.moveTo(rect.getLeft() + 2, rect.getBottom() + 2);
            cb.lineTo(rect.getLeft() + 12, rect.getBottom() + 12);

            cb.moveTo(rect.getLeft() + 2, rect.getBottom() + 12);
            cb.lineTo(rect.getLeft() + 12, rect.getBottom() + 2);

            cb.stroke();
        }
    }
}
