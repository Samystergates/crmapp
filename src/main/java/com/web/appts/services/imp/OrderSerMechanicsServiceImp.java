package com.web.appts.services.imp;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderSERDto;
import com.web.appts.entities.OrderSER;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.OrderSERRepo;
import com.web.appts.services.OrderSerMechanicsService;
import com.web.appts.utils.MonteurData;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderSerMechanicsServiceImp implements OrderSerMechanicsService {

    Map<String, OrderSERDto> orderSERMap = new HashMap();

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OrderSERRepo orderSERRepo;

    @Autowired
    private OrderServiceImp orderServiceImp;


    @Override
    public OrderSERDto createOrderSER(OrderSERDto orderSERDto) {
        OrderSERDto orderSERDtoMapVal = this.getOrderSER(orderSERDto.getOrderNumber());
        if (orderSERDtoMapVal == null) {
            OrderSER orderSERSaved = (OrderSER) this.orderSERRepo.save(this.dtoToSER(orderSERDto));
            this.orderSERMap.put(orderSERSaved.getOrderNumber(), this.serToDto(orderSERSaved));
            return this.serToDto(orderSERSaved);
        } else {
            orderSERDto.setId(orderSERDtoMapVal.getId());
            return this.updateOrderSER(orderSERDto);
        }
    }

    @Override
    public OrderSERDto updateOrderSER(OrderSERDto orderSERDto) {
        OrderSER orderSERUpdated = (OrderSER) this.orderSERRepo.save(this.dtoToSER(orderSERDto));
        this.orderSERMap.put(orderSERUpdated.getOrderNumber(), orderSERDto);
        return this.serToDto(orderSERUpdated);
    }

    @Override
    public OrderSERDto getOrderSER(String orderNumber) {
        if (!this.orderSERMap.isEmpty() && this.orderSERMap.containsKey(orderNumber)) {
            return (OrderSERDto) this.orderSERMap.get(orderNumber);
        } else {
            OrderSER orderSER = this.orderSERRepo.findByOrderNumber(orderNumber);
            return orderSER == null ? null : this.serToDto(orderSER);
        }
    }

    @Override
    public Boolean deleteOrderSER(Long orderSERId) {
        OrderSER orderSER = (OrderSER) this.orderSERRepo.findById(orderSERId).orElseThrow(() -> {
            return new ResourceNotFoundException("orderSER", "id", (long) orderSERId.intValue());
        });
        this.orderSERMap.remove(orderSER.getOrderNumber());
        this.orderSERRepo.delete(orderSER);
        return true;
    }

    @Override
    public List<OrderSERDto> getAllSer() {
        List<OrderSER> listSer = this.orderSERRepo.findAll();
        return (List) listSer.stream().map((sme) -> {
            return this.serToDto(sme);
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllMonteurs() {
        return MonteurData.getAllMonteurs();
    }

    public OrderSERDto serToDto(OrderSER orderSER) {
        return (OrderSERDto) this.modelMapper.map(orderSER, OrderSERDto.class);
    }

    public OrderSER dtoToSER(OrderSERDto orderSERDto) {
        return (OrderSER) this.modelMapper.map(orderSERDto, OrderSER.class);
    }

    @Override
    public byte[] generateSERPdf(String key) {
        try {
            List<OrderDto> orderDtos = new ArrayList<>();
            orderServiceImp.getMap().values().forEach(e -> {
                if (e.getOrderNumber().equals(key)) {
                    orderDtos.add(e);
                }
            });

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
//            PdfWriter.getInstance(document, outputStream);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD);
            Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            // === 1. HEADER ROW ===
            PdfPTable headerTable = new PdfPTable(new float[]{1, 2});
            headerTable.setWidthPercentage(100);

            // Logo on left
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("images/demolen.jpg");
            Image logo = Image.getInstance(ImageIO.read(imageStream), null);
            logo.scaleToFit(120, 60);
            PdfPCell logoCell = new PdfPCell(logo, false);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_TOP);
            headerTable.addCell(logoCell);

            // Company name
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);

            Paragraph companyName = new Paragraph("DE MOLEN BANDEN B.V", titleFont);
            companyName.setAlignment(Element.ALIGN_RIGHT);
            companyName.setSpacingAfter(10f);
            companyCell.addElement(companyName);

            LineSeparator line = new LineSeparator();
            line.setLineWidth(1f);
            line.setPercentage(107f);
            line.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(line);
            headerTable.addCell(companyCell);

            document.add(headerTable);

            document.add(Chunk.NEWLINE);

            // === 2. SECOND ROW (SERVICEBON + ADDRESSES & INFO) ===
            PdfPTable secondRow = new PdfPTable(new float[]{2, 2});
            secondRow.setWidthPercentage(100);

            // Left column: SERVICEBON + addresses stacked
            PdfPCell leftCol = new PdfPCell();
            leftCol.setBorder(Rectangle.NO_BORDER);

            Paragraph servicebon = new Paragraph("SERVICEBON", subTitleFont);
            servicebon.setSpacingAfter(10);
            servicebon.setIndentationLeft(18f);
            leftCol.addElement(servicebon);

            OrderDto orderDto = orderDtos.get(0);
//address structure:
            // naam
            // straat huisnr additioneel
            // postcode plaats
            // land with https://restcountries.com/v3.1/alpha?codes=NL
            PdfPTable addressStack = new PdfPTable(1);
            addressStack.setWidthPercentage(85);
            addressStack.setSpacingAfter(5f);
            addressStack.setHorizontalAlignment(Element.ALIGN_CENTER);
//            PdfPCell factuurCell = new PdfPCell(new Paragraph("Factuuradres:\n" + orderDto.getCustomerName() + "\n" + orderDto.getStreet() + " " + orderDto.getHouseNR() + " " + orderDto.getAdditionalAdd() + "\n" + orderDto.getPostCode() + " " + orderDto.getCity() + "\n" + orderDto.getCountry(), normalFont));
//            factuurCell.setPaddingTop(10f);

            Font boldFont1 = new Font(normalFont);
            boldFont1.setStyle(Font.BOLD);
            String country = orderDto.getCountry();
            if(country.equals("NL")){
                country = "Nederland";
            }

            Paragraph factuurParagraph = new Paragraph();
            factuurParagraph.add(new Chunk("Factuuradres:\n", boldFont1));
            factuurParagraph.add(new Chunk(
                    orderDto.getCustomerName() + "\n" +
                            orderDto.getStreet() + " " + orderDto.getHouseNR() + " " + orderDto.getAdditionalAdd() + "\n" +
                            orderDto.getPostCode() + " " + orderDto.getCity() + "\n" +
                            country, normalFont
            ));

            PdfPCell factuurCell = new PdfPCell(factuurParagraph);
            factuurCell.setPaddingTop(10f);

            factuurCell.setPaddingLeft(10f);
            factuurCell.setPaddingBottom(16f);
            addressStack.addCell(factuurCell);

            PdfPTable addressStack2 = new PdfPTable(1);
            addressStack2.setWidthPercentage(85);
            addressStack2.setHorizontalAlignment(Element.ALIGN_CENTER);
//            PdfPCell afleverCell = new PdfPCell(new Paragraph("Afleveradres:\n" + orderDto.getCustomerName() + "\n" + orderDto.getStreet() + " " + orderDto.getHouseNR() + " " + orderDto.getAdditionalAdd() + "\n" + orderDto.getPostCode() + " " + orderDto.getCity() + "\n" + orderDto.getCountry(), normalFont));
//            afleverCell.setPaddingTop(10f);


            Paragraph afleverParagraph = new Paragraph();
            afleverParagraph.add(new Chunk("Afleveradres:\n", boldFont1));
            afleverParagraph.add(new Chunk(
                    orderDto.getCustomerName() + "\n" +
                            orderDto.getStreet() + " " + orderDto.getHouseNR() + " " + orderDto.getAdditionalAdd() + "\n" +
                            orderDto.getPostCode() + " " + orderDto.getCity() + "\n" +
                            country, normalFont
            ));

            PdfPCell afleverCell = new PdfPCell(afleverParagraph);
            afleverCell.setPaddingTop(10f);

            afleverCell.setPaddingLeft(10f);
            afleverCell.setPaddingBottom(16f);
            addressStack2.addCell(afleverCell);
            addressStack2.setSpacingAfter(6f);

            leftCol.addElement(addressStack);
            leftCol.addElement(addressStack2);
            secondRow.addCell(leftCol);

            // Right column: Company info small + Nummer/Afnemer/Datum
            PdfPCell rightCol = new PdfPCell();
            rightCol.setBorder(Rectangle.NO_BORDER);
            PdfPTable infoStack = new PdfPTable(1);
            Paragraph companyInfo = new Paragraph("Vestiging Reek\nRustvenseweg 2 5375 KW REEK\nTel: 0486 - 479797 Fax: 0486 - 472845\nSwift: RABONL2U\nIBAN: NL13 RABO 0140 4015 98\nK.v.K.: 160 620 75\nBTWnr. NL009753758B01", normalFont);
            companyInfo.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell infoCell = new PdfPCell(companyInfo);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoCell.setPaddingRight(0);
            infoStack.addCell(infoCell);
            infoStack.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoStack.setSpacingAfter(35f);
            rightCol.addElement(infoStack);

            PdfPTable numTable = new PdfPTable(3);
            numTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            numTable.setWidthPercentage(70);
            numTable.addCell(createCell("Nummer\n" + orderDto.getOrderNumber(), normalFont, true));
            numTable.addCell(createCell("Afnemer\n" + orderDto.getProduct(), normalFont, true));
            String dateStr = orderDto.getDeliveryDate();
            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                String formatted = parts[2] + "-" + parts[1] + "-" + parts[0];
                numTable.addCell(createCell("Datum\n" + formatted, normalFont, true));
            } else {
                numTable.addCell(createCell("Datum\n" + orderDto.getDeliveryDate(), normalFont, true));
            }
            rightCol.addElement(numTable);

            secondRow.addCell(rightCol);
            document.add(secondRow);

            document.add(Chunk.NEWLINE);

            // === 3. PRODUCTS TABLE ===
            PdfPTable productTable = new PdfPTable(new float[]{2, 10, 1, 1, 2, 3});
            productTable.setWidthPercentage(100);

            String[] headers = {"Product", "Omschrijving", "Best", "Gel.", "Nog.lev.", "Leverdatum"};
            for (int i = 0; i < headers.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(headers[i], boldFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPaddingRight(3f);
                if (i >= 2) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                productTable.addCell(cell);
            }

            for (OrderDto row : orderDtos) {
                PdfPCell cell1 = new PdfPCell(new Phrase(row.getProduct(), normalFont));
                PdfPCell cell2 = new PdfPCell(new Phrase(row.getOmsumin(), normalFont));
//                String input = row.getAantal();
//                String firstLine = input.split("\\R")[0];
//                String beforeDecimal = firstLine.replaceAll("\\..*", ""); // remove dot and everything after

                int aantal = this.extractIntegerPart(row.getAantal());
                int gel = this.extractIntegerPart(row.getGel());
                int novgel = aantal - gel;

                PdfPCell cell3 = new PdfPCell(new Phrase(String.valueOf(aantal), normalFont));
                PdfPCell cell4 = new PdfPCell(new Phrase(String.valueOf(gel), normalFont));
                PdfPCell cell5 = new PdfPCell(new Phrase(String.valueOf(novgel), normalFont));
                String dateStr1 = row.getDeliveryDate();
                String[] parts1 = dateStr1.split("-");
                PdfPCell cell6;
                if (parts1.length == 3) {
                    String formatted = parts1[2] + "-" + parts1[1] + "-" + parts1[0];
                    cell6 = new PdfPCell(new Phrase(formatted, normalFont));
                } else {
                    cell6 = new PdfPCell(new Phrase(row.getDeliveryDate(), normalFont));
                }

                cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell5.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell6.setHorizontalAlignment(Element.ALIGN_RIGHT);


                // remove all borders for data cells
                cell1.setBorder(Rectangle.NO_BORDER);
                cell2.setBorder(Rectangle.NO_BORDER);
                cell3.setBorder(Rectangle.NO_BORDER);
                cell4.setBorder(Rectangle.NO_BORDER);
                cell5.setBorder(Rectangle.NO_BORDER);
                cell6.setBorder(Rectangle.NO_BORDER);

                productTable.addCell(cell1);
                productTable.addCell(cell2);
                productTable.addCell(cell3);
                productTable.addCell(cell4);
                productTable.addCell(cell5);
                productTable.addCell(cell6);

            }
            document.add(productTable);

            document.add(Chunk.NEWLINE);

            PdfContentByte cb = writer.getDirectContent();

            PdfPTable footerFields = new PdfPTable(new float[]{1, 1});
            footerFields.setTotalWidth(PageSize.A4.getWidth() - 72); // 36 margin left + 36 margin right
            footerFields.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPTable leftFooterTable = new PdfPTable(1);
            leftFooterTable.setWidthPercentage(100);
            leftFooterTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            leftFooterTable.addCell(createCell1("Extra band gemonteerd: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createCell1("Nieuw / Gebruikt / Eigen", normalFont, false));
            leftFooterTable.addCell(createLabeledLine("Maat:", normalFont));

            leftFooterTable.addCell(createCell1("Extra band gerepareerd: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createLabeledLine("Aantal:", normalFont));

            leftFooterTable.addCell(createCell1("Extra band afgevoerd: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createLabeledLine("Maat:", normalFont));

            leftFooterTable.addCell(createCell1("Extra binnenband: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createLabeledLine("Maat:", normalFont));

            leftFooterTable.addCell(createCell1("Manchet gebruikt: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createCell1("Ventiel gebruikt: Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createCell1("O-Ring: gemonteerd Ja / Nee", normalFont, false));
            leftFooterTable.addCell(createLabeledLine("Bandendichter aantal liter:", normalFont));

            PdfPCell leftFooter = new PdfPCell(leftFooterTable);
            leftFooter.setBorder(Rectangle.NO_BORDER);
            footerFields.addCell(leftFooter);


            PdfPTable rightFooterTable = new PdfPTable(1);
            rightFooterTable.setWidthPercentage(100);
            rightFooterTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            rightFooterTable.addCell(createLabeledLine("Kenteken en/of voertuig:", normalFont));
            rightFooterTable.addCell(createLabeledLine("KM of Urenstand machine:", normalFont));
            rightFooterTable.addCell(createLabeledLine("Naam monteur De Molen Banden B.V.:", normalFont));
            rightFooterTable.addCell(createLabeledLine("Gereed kilometers", normalFont));
            rightFooterTable.addCell(createLabeledLine("Gewerkte kwartieren", normalFont));

            PdfPCell rightFooter = new PdfPCell(rightFooterTable);
            rightFooter.setVerticalAlignment(Element.ALIGN_BOTTOM);
            rightFooter.setBorder(Rectangle.NO_BORDER);
            footerFields.addCell(rightFooter);

            rightFooter.setBorder(Rectangle.NO_BORDER);
            footerFields.addCell(rightFooter);

            footerFields.writeSelectedRows(0, -1, 36, 350, cb);

            PdfPTable bottomSign = new PdfPTable(new float[]{1, 1, 1});
            bottomSign.setTotalWidth(PageSize.A4.getWidth() - 72);

            PdfPCell cell1 = createCell("Voor akkoord:", normalFont, true);
            cell1.setPaddingBottom(35f);
            cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
            bottomSign.addCell(cell1);
            PdfPCell cell2 = createCell("Naam: (Blokletters)", normalFont, true);
            cell2.setPaddingBottom(35f);
            cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            bottomSign.addCell(cell2);
            PdfPCell cell3 = createCell("30 dagen na factuurdatum\nFranco vrij", boldFont, true);
            cell3.setPaddingBottom(35f);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            bottomSign.addCell(cell3);

// Place above note
            bottomSign.writeSelectedRows(0, -1, 36, 90, cb);

            Paragraph note = new Paragraph("Let op: Wielmoeren/bouten na montage natrekken", boldFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(note),
                    PageSize.A4.getWidth() / 2, 20, 0);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private PdfPCell createCell(String text, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPaddingLeft(5f);
        cell.setPaddingBottom(5f);
        if (!border) cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell createCell1(String text, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (!border) cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell createLabeledLine(String label, Font font) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, font));
        p.add(Chunk.NEWLINE);

        // Add a full-width line for writing
        LineSeparator line = new LineSeparator();
        line.setAlignment(Element.ALIGN_LEFT);
        line.setLineWidth(0.5f);     // thickness
        line.setPercentage(50f);    // full cell width
        line.setOffset(-2f);         // vertical offset from text
        p.add(line);

        p.setSpacingAfter(10f); // space for handwriting
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    public int extractIntegerPart(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        // Take the first line only
        String firstLine = input.split("\\R")[0].trim();

        // If it has a decimal, split and take the integer part
        if (firstLine.contains(".")) {
            String beforeDecimal = firstLine.split("\\.")[0];
            return Integer.parseInt(beforeDecimal);
        }

        // If no decimal, parse directly
        return Integer.parseInt(firstLine);
    }

    private PdfPCell createLabeledLine1(String label, Font font) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, font));
        p.add(Chunk.NEWLINE);

        LineSeparator line = new LineSeparator();
        line.setAlignment(Element.ALIGN_LEFT);
        line.setLineWidth(0.5f);
        line.setPercentage(100f);  // span entire cell width
        line.setOffset(-2f);
        p.add(line);

        p.setSpacingAfter(8f); // space for handwriting
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }


}
