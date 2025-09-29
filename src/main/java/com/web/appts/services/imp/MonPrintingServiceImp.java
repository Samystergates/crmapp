
package com.web.appts.services.imp;

import com.itextpdf.text.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.*;
import com.web.appts.DTO.OrderDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import com.web.appts.controllers.CheckboxCellEvent;
import com.web.appts.entities.MonSubOrders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonPrintingServiceImp {
    @Autowired
    OrderServiceImp orderServiceImp;

    public MonPrintingServiceImp() {
    }

    public byte[] generateMONPdf(String key) {
        Map<String, OrderDto> ordersMap = this.orderServiceImp.getMap();
        List<OrderDto> orderList = null;

        orderList = (List) ordersMap.values().stream().filter((orderDto) -> {
            return key.equals(orderDto.getOrderNumber()) && ("Y".equals(orderDto.getMonLb()) || "Y".equals(orderDto.getMonTr()));
        }).collect(Collectors.toList());

        if (orderList == null || orderList.size() == 0) {
            orderList = (List) ordersMap.values().stream().filter((orderDto) -> {
                return key.equals(orderDto.getOrderNumber());
            }).collect(Collectors.toList());
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] var7;
            try {
                Document document = new Document();
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                document.open();
                this.addHeadingAndAddress(document, "Montage Order");
                this.addBloeHeadingAndInfo(writer, document, "Klantnaam", orderList);
                this.addOrdersTable(document, orderList);
                this.addOptions(writer, document, orderList);
                this.addSectionsAtFixedPosition(writer);
                System.out.println("Closing Document");
                document.close();
                var7 = outputStream.toByteArray();
            } catch (Throwable var9) {
                try {
                    outputStream.close();
                } catch (Throwable var8) {
                    var9.addSuppressed(var8);
                }

                throw var9;
            }

            outputStream.close();
            return var7;
        } catch (IOException | DocumentException var10) {
            Exception e = var10;
            ((Exception) e).printStackTrace();
            return new byte[0];
        }
    }

    private void addHeadingAndAddress(Document document, String heading) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 18.0F, 1);
        new Font(FontFamily.HELVETICA, 10.0F);
        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Paragraph paragraph = new Paragraph("", font);
        paragraph.setAlignment(2);
        cell1.addElement(paragraph);
        cell1.setBorder(0);
        mainTable.addCell(cell1);
        PdfPCell cell2 = new PdfPCell();
        Paragraph paragraph2 = new Paragraph(heading, font);
        paragraph2.setAlignment(1);
        cell2.addElement(paragraph2);
        cell2.setBorder(0);
        mainTable.addCell(cell2);
        PdfPCell cell3 = new PdfPCell();
        Paragraph paragraph3 = new Paragraph("De Molen Banden B.V.", font3);
        Paragraph paragraph4 = new Paragraph("Rustvenseweg 2\n5375 KW REEK", font4);
        paragraph3.setAlignment(2);
        paragraph4.setAlignment(2);
        cell3.addElement(paragraph3);
        cell3.addElement(paragraph4);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        mainTable.setSpacingAfter(10.0F);
        document.add(mainTable);
    }

    private void addBloeHeadingAndInfo(PdfWriter writer, Document document, String heading, List<OrderDto> list) throws DocumentException {
        new Font(FontFamily.HELVETICA, 18.0F, 1);
        Font font2 = new Font(FontFamily.HELVETICA, 12.0F);
        new Font(FontFamily.HELVETICA, 10.0F, 1);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Barcode128 barcode = new Barcode128();
        barcode.setCode(((OrderDto) list.get(0)).getOrderNumber());
        barcode.setFont((BaseFont) null);
        barcode.setBaseline(0.0F);
        barcode.setBarHeight(15.0F);
        PdfContentByte cb = writer.getDirectContent();
        Image image = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        image.setAlignment(0);
        image.scalePercent(120.0F);
        cell1.addElement(image);
        Paragraph labelParagraph = new Paragraph(String.format("%-9s%-9s", " ", ((OrderDto) list.get(0)).getOrderNumber()), font2);
        labelParagraph.setAlignment(0);
        cell1.addElement(labelParagraph);
        cell1.setBorder(0);
        mainTable.addCell(cell1);
        PdfPCell cell2 = new PdfPCell();
        Paragraph paragraphL1 = new Paragraph(String.format("%-12s%-12s", heading + ":", ((OrderDto) list.get(0)).getCustomerName()), font4);

        paragraphL1.setAlignment(Element.ALIGN_CENTER);

        cell2.addElement(paragraphL1);

        cell2.setBorder(0);
        cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        mainTable.addCell(cell2);
        PdfPCell cell3 = new PdfPCell();
        Paragraph paragraphR1 = new Paragraph(String.format("%-10s%-10s", "Verkooporder: ", ((OrderDto) list.get(0)).getOrderNumber()), font4);
        Paragraph paragraphR2 = new Paragraph(String.format("%-10s%-10s", "Behandelaar: ", ((OrderDto) list.get(0)).getUser()), font4);
        Paragraph paragraphR3 = new Paragraph(String.format("%-10s%-10s", "Leverdatum: ", ((OrderDto) list.get(0)).getDeliveryDate()), font4);
        paragraphR1.setAlignment(2);
        paragraphR2.setAlignment(2);
        paragraphR3.setAlignment(2);
        cell3.addElement(paragraphR1);
        cell3.addElement(paragraphR2);
        cell3.addElement(paragraphR3);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        mainTable.setSpacingAfter(30.0F);
        document.add(mainTable);
    }


    private void addOrdersTable(Document document, List<OrderDto> orderList) throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        table.setKeepTogether(false);
        table.setSplitLate(false);
        table.setSplitRows(true);

        table.setWidthPercentage(100.0F);
        float[] columnWidths = new float[]{4.0F, 5.0F, 11.0F, 80.0F};
        table.setWidths(columnWidths);

        Font font2 = new Font(FontFamily.HELVETICA, 10.0F);
        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
        PdfPCell headerCell = this.createHeaderCell("Rgl", font3);
        PdfPCell headerCell2 = this.createHeaderCell("Best", font3);
        PdfPCell headerCell3 = this.createHeaderCell("Product", font3);
        PdfPCell headerCell4 = this.createHeaderCell("Omschrijving", font3);
        table.addCell(headerCell);
        table.addCell(headerCell2);
        table.addCell(headerCell3);
        table.addCell(headerCell4);

        orderList.sort(Comparator.comparing(order -> Integer.parseInt(order.getRegel())));
        Stack<Integer> orderTrack = new Stack<>();
        for (int i = 0; i < orderList.size(); ++i) {
//        for (int i = 0; i < 9; ++i) {

            Boolean isRepeat = false;
            if (!orderTrack.empty() && orderTrack.peek() == i) {
                isRepeat = true;
            } else {
                orderTrack.add(i);
            }
            if (i < orderList.size()) {
                List<MonSubOrders> list = orderList.get(i).getMonSubOrders();

                PdfPCell cell1;
                PdfPCell cell2;
                PdfPCell cell3;
                PdfPCell cell4;
                if (!isRepeat) {
                    String[] aantal;
                    if (orderList.get(i).getAantal().contains(".")) {
                        aantal = orderList.get(i).getAantal().split("\\.");
                        cell2 = this.createCell(aantal[0], font2);
                    } else {
                        cell2 = this.createCell(((OrderDto) orderList.get(i)).getAantal(), font2);
                    }
                    cell1 = this.createCell(((OrderDto) orderList.get(i)).getRegel(), font2);
                    cell3 = this.createCell(((OrderDto) orderList.get(i)).getProduct(), font2);
                    if (orderList.get(i).getTekst() != null && !orderList.get(i).getTekst().isEmpty()) {
                        cell4 = this.createCellWithNewLine(((OrderDto) orderList.get(i)).getOmsumin(), orderList.get(i).getTekst(), font2);
                    } else {
                        cell4 = this.createCellOms(((OrderDto) orderList.get(i)).getOmsumin(), font2);
                    }
                } else {
                    cell1 = this.createCell("", font2);
                    cell2 = this.createCell("", font2);
                    cell3 = this.createCell("", font2);
                    cell4 = this.createCell("", font2);
                }

                if(list == null){
                    list = new ArrayList<>();
                }

                if (list.size() != 0 && !isRepeat) {
                    --i;
                    table.addCell(cell1);
                    table.addCell(cell2);
                    table.addCell(cell3);
                    table.addCell(cell4);

                    cell1.setFixedHeight(20.0F);
                    cell2.setFixedHeight(20.0F);
                    cell3.setFixedHeight(20.0F);
                    cell4.setFixedHeight(20.0F);
                    continue;
                }


                if (list.size() != 0 && isRepeat) {
                    orderTrack.pop();
                    PdfPTable nestedTable = new PdfPTable(3);
                    nestedTable.setWidthPercentage(100.0F);
                    float[] nestedColumnWidths = new float[]{6.0F, 12.0F, 82.0F};
                    nestedTable.setWidths(nestedColumnWidths);
                    nestedTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                    for (MonSubOrders subOrder : list) {
                        PdfPCell nestedCell1 = null;
                        String[] aantal;
                        if (orderList.get(i).getAantal().contains(".")) {
                            aantal = orderList.get(i).getAantal().split("\\.");
                            nestedCell1 = this.createCell(aantal[0], font2);
                        } else {
                            nestedCell1 = this.createCell(subOrder.getAantal(), font2);
                        }
                        PdfPCell nestedCell2 = this.createCell(subOrder.getProduct(), font2);
                        PdfPCell nestedCell3 = this.createInnerOmsCell(subOrder.getOmsumin(), font2);
                        nestedCell1.setBorder(Rectangle.RIGHT | Rectangle.BOTTOM);
                        nestedCell2.setBorder(Rectangle.RIGHT | Rectangle.BOTTOM);
                        nestedCell3.setBorder(Rectangle.BOTTOM);
                        nestedTable.addCell(nestedCell1);
                        nestedTable.addCell(nestedCell2);
                        nestedTable.addCell(nestedCell3);
                    }

                    cell4.addElement(nestedTable);
                }

                table.addCell(cell1);
                table.addCell(cell2);
                table.addCell(cell3);
                table.addCell(cell4);

                cell1.setFixedHeight(20.0F);
                cell2.setFixedHeight(20.0F);
                cell3.setFixedHeight(20.0F);
                cell4.setFixedHeight(20.0F);
            }
        }

        PdfPTable outerTable = new PdfPTable(1);
        outerTable.setWidthPercentage(100.0f);


        PdfPCell tableWrapper = new PdfPCell(table);
//        tableWrapper.setPaddingBottom(270f);

        tableWrapper.setPaddingBottom(0f);
        tableWrapper.setBorder(Rectangle.NO_BORDER);
        outerTable.addCell(tableWrapper);

        document.add(outerTable);
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell headerCell = new PdfPCell(new Phrase(text, font));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(1);
        headerCell.setBorderWidth(0.3F);
        headerCell.setPadding(2.0F);
        return headerCell;
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(1);
        cell.setVerticalAlignment(5);
        cell.setBorderWidth(0.3F);
        cell.setPadding(2.0F);
        return cell;
    }

    private PdfPCell createInnerOmsCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_LEFT);
        cell.setBorderWidth(0.3F);
        cell.setPadding(2.0F);
        return cell;
    }

    private PdfPCell createCellOms(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);  // Align text to the left
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);  // Optional: aligns text vertically to middle
        cell.setBorderWidth(0.3F);
        cell.setPadding(2.0F);
        return cell;
    }

    private PdfPCell createCellWithNewLine(String text, String text2, Font font) {

        Paragraph paragraph1 = new Paragraph(text, font);
        paragraph1.setAlignment(Element.ALIGN_CENTER);

        Font smallerFont = new Font(font.getFamily(), font.getSize() - 1, font.getStyle());
        Paragraph paragraph2 = new Paragraph(text2, smallerFont);
        paragraph2.setAlignment(Element.ALIGN_LEFT);
        paragraph2.setSpacingBefore(5);

        Phrase phrase = new Phrase();
        phrase.add(paragraph1);
        phrase.add(new Chunk("\n"));
        phrase.add(paragraph2);

        PdfPCell cell = new PdfPCell(phrase);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderWidth(0.3F);
        cell.setPadding(2.0F);

        return cell;
    }


    private void addOptions(PdfWriter writer, Document document, List<OrderDto> orderList) throws DocumentException {

        Font font3 = new Font(Font.FontFamily.HELVETICA, 10.0F, Font.BOLD);
        PdfPTable table = new PdfPTable(6);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidthPercentage(75);
        table.setWidths(new float[]{4, 2, 2, 2, 2, 5});

        table.addCell(createTextCell("Band(en) schoon: "));
        table.addCell(createTextCell(""));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("JA"));
        table.addCell(createTextCell(""));
        table.addCell(createTextCell(""));

        PdfPCell spacerCell1 = new PdfPCell(new Phrase(""));
        spacerCell1.setColspan(6);
        spacerCell1.setFixedHeight(10f);
        spacerCell1.setBorder(Rectangle.NO_BORDER);
        table.addCell(spacerCell1);


        table.addCell(createTextCell(""));
        table.addCell(createTextCell(""));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("NEE"));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("Schoon gemaakt."));

        PdfPCell spacerCell2 = new PdfPCell(new Phrase(""));
        spacerCell2.setColspan(6);
        spacerCell2.setFixedHeight(10f);
        spacerCell2.setBorder(Rectangle.NO_BORDER);
        table.addCell(spacerCell2);

        table.addCell(createTextCell("Beschadigingen: "));
        table.addCell(createTextCell(""));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("NEE"));
        table.addCell(createTextCell(""));
        table.addCell(createTextCell(""));

        PdfPCell spacerCell3 = new PdfPCell(new Phrase(""));
        spacerCell3.setColspan(6);
        spacerCell3.setFixedHeight(10f);
        spacerCell3.setBorder(Rectangle.NO_BORDER);
        table.addCell(spacerCell3);

        table.addCell(createTextCell(""));
        table.addCell(createTextCell(""));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("JA"));
        table.addCell(createCheckboxCell(false));
        table.addCell(createTextCell("Bijgewerkt"));

        PdfContentByte canvas = writer.getDirectContent();
        ColumnText columnText = new ColumnText(canvas);

        Paragraph title = new Paragraph("Product eindcontrole:", font3);
        columnText.setSimpleColumn(36, 60, 559, 260); // Left, Bottom, Right, Top coordinates
        columnText.addElement(title);
        columnText.addElement(table);
        columnText.go();
    }

//    private void addOptions(PdfWriter writer, Document document, List<OrderDto> orderList) throws DocumentException {
//
//        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
//        Paragraph labelParagraph0 = new Paragraph("Product eindcontrole:", font3);
//        PdfPTable table = new PdfPTable(6);
//        table.setWidthPercentage(75);
//        table.setHorizontalAlignment(Element.ALIGN_LEFT);
//        table.setWidths(new float[]{4,2,2,2,2,5});
//
//        table.addCell(createTextCell("Band(en) schoon: "));
//        table.addCell(createTextCell(""));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("JA"));
//        table.addCell(createTextCell(""));
//        table.addCell(createTextCell(""));
//
//        PdfPCell spacerCell1 = new PdfPCell(new Phrase(""));
//        spacerCell1.setColspan(6);
//        spacerCell1.setFixedHeight(10f);
//        spacerCell1.setBorder(Rectangle.NO_BORDER);
//        table.addCell(spacerCell1);
//
//
//        table.addCell(createTextCell(""));
//        table.addCell(createTextCell(""));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("NEE"));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("Schoon gemaakt."));
//
//        PdfPCell spacerCell2 = new PdfPCell(new Phrase(""));
//        spacerCell2.setColspan(6);
//        spacerCell2.setFixedHeight(10f);
//        spacerCell2.setBorder(Rectangle.NO_BORDER);
//        table.addCell(spacerCell2);
//
//        table.addCell(createTextCell("Beschadigingen: "));
//        table.addCell(createTextCell(""));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("NEE"));
//        table.addCell(createTextCell(""));
//        table.addCell(createTextCell(""));
//
//        PdfPCell spacerCell3 = new PdfPCell(new Phrase(""));
//        spacerCell3.setColspan(6);
//        spacerCell3.setFixedHeight(10f);
//        spacerCell3.setBorder(Rectangle.NO_BORDER);
//        table.addCell(spacerCell3);
//
//        table.addCell(createTextCell(""));
//        table.addCell(createTextCell(""));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("JA"));
//        table.addCell(createCheckboxCell(false));
//        table.addCell(createTextCell("Bijgewerkt"));
//
//        table.setSpacingAfter(15.0F);
//        document.add(labelParagraph0);
//        document.add(table);
//    }

    private PdfPCell createTextCell(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 11); // Set font size
        PdfPCell cell = new PdfPCell(new Phrase(text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_MIDDLE); // Align text to the right
        return cell;
    }

    private PdfPCell createCheckboxCell(boolean isChecked) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(12f);
        cell.setCellEvent(new CheckboxCellEvent(isChecked));
        return cell;
    }

    private void addSectionsAtFixedPosition(PdfWriter writer) throws DocumentException {
        Font font4 = new Font(Font.FontFamily.HELVETICA, 10.0F);

        // First section: "Opmerking"
        PdfPTable table1 = new PdfPTable(1);
        table1.setTotalWidth(520f); // Full width (adjust to match page width)
        PdfPCell cell1 = new PdfPCell();
        cell1.setBorder(Rectangle.BOX);
        cell1.setBorderColor(BaseColor.BLACK);
        cell1.setBorderWidth(1.0F);
        cell1.setMinimumHeight(60.0F);
        Paragraph textParagraph = new Paragraph("Opmerking:", font4);
        cell1.addElement(textParagraph);
        table1.addCell(cell1);

        // Second section: "Paraaf en naam monteur"
        PdfPTable table2 = new PdfPTable(1);
        table2.setTotalWidth(520f); // Same width for alignment
        PdfPCell cell2 = new PdfPCell();
        cell2.setBorder(Rectangle.BOX);
        cell2.setBorderColor(BaseColor.BLACK);
        cell2.setBorderWidth(1.0F);
        cell2.setMinimumHeight(60.0F);
        Paragraph textParagraph2 = new Paragraph("Paraaf en naam monteur:", font4);
        cell2.addElement(textParagraph2);
        table2.addCell(cell2);

        PdfContentByte canvas = writer.getDirectContent();

        // Place first table (Opmerking)
        table1.writeSelectedRows(0, -1, 36, 140, canvas); // X=36 (left margin), Y=120 (from bottom)

        // Place second table just below the first with small gap
        table2.writeSelectedRows(0, -1, 36, 70, canvas); // Y=50 means ~10pts gap from table1
    }


//    private void addSections(Document document) throws DocumentException {
//        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
//        PdfPTable table = new PdfPTable(1);
//        table.setWidthPercentage(100.0F);
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(15);
//        cell.setBorderColor(BaseColor.BLACK);
//        cell.setBorderWidth(1.0F);
//        cell.setMinimumHeight(60.0F);
//        Paragraph textParagraph = new Paragraph("Opmerking:", font4);
//        cell.addElement(textParagraph);
//        table.addCell(cell);
//        table.setSpacingAfter(10.0F);
//        PdfPTable table2 = new PdfPTable(1);
//        table2.setWidthPercentage(100.0F);
//        PdfPCell cell2 = new PdfPCell();
//        cell2.setBorder(15);
//        cell2.setBorderColor(BaseColor.BLACK);
//        cell2.setBorderWidth(1.0F);
//        cell2.setMinimumHeight(60.0F);
//        Paragraph textParagraph2 = new Paragraph("Paraaf en naam monteur:", font4);
//        cell2.addElement(textParagraph2);
//        table2.addCell(cell2);
//        document.add(table);
//        document.add(table2);
//    }
}
