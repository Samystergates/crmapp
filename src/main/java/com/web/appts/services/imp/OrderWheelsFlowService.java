
package com.web.appts.services.imp;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderSMEDto;
import com.web.appts.DTO.OrderSPUDto;
import com.web.appts.DTO.WheelColorDto;
import com.web.appts.entities.OrderSME;
import com.web.appts.entities.OrderSPU;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.OrderSMERepo;
import com.web.appts.repositories.OrderSPURepo;
import com.web.appts.services.OrderSMEService;
import com.web.appts.services.OrderSPUService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.web.appts.utils.AanOptions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderWheelsFlowService implements OrderSMEService, OrderSPUService {
    @Autowired
    OrderSPURepo orderSPURepo;
    @Autowired
    OrderSMERepo orderSMERepo;
    @Autowired
    OrderServiceImp orderServiceImp;
    @Autowired
    WheelServices wheelServices;
    Map<String, OrderSMEDto> orderSMEMap = new HashMap();
    Map<String, OrderSPUDto> orderSPUMap = new HashMap();
    @Autowired
    private ModelMapper modelMapper;

    public OrderWheelsFlowService() {
    }

    public OrderSPUDto createOrderSPU(OrderSPUDto orderSPUDto) {
        OrderSPUDto orderSPUDtoMapVal = this.getOrderSPU(orderSPUDto.getOrderNumber(), orderSPUDto.getRegel());
        if (orderSPUDtoMapVal == null) {
            OrderDto orderDto = (OrderDto) this.orderServiceImp.getMap().get(orderSPUDto.getOrderNumber() + "," + orderSPUDto.getRegel());
            orderDto.setSpu("R");
            this.orderServiceImp.updateOrder(orderDto, orderDto.getId(), true);
            OrderSPU orderSPUSaved = (OrderSPU) this.orderSPURepo.save(this.dtoToSPU(orderSPUDto));
            this.orderSPUMap.put(orderSPUSaved.getOrderNumber() + " - " + orderSPUSaved.getRegel(), this.spuToDto(orderSPUSaved));
            return this.spuToDto(orderSPUSaved);
        } else {
            orderSPUDto.setId(orderSPUDtoMapVal.getId());
            return this.updateOrderSPU(orderSPUDto);
        }
    }

    public OrderSPUDto updateOrderSPU(OrderSPUDto orderSPUDto) {
        OrderSPU orderSPUUpdated = (OrderSPU) this.orderSPURepo.save(this.dtoToSPU(orderSPUDto));
        this.orderSPUMap.put(orderSPUUpdated.getOrderNumber() + " - " + orderSPUUpdated.getRegel(), orderSPUDto);
        return this.spuToDto(orderSPUUpdated);
    }

    public Boolean deleteOrderSPU(Long orderSPUId) {
        OrderSPU orderSPU = (OrderSPU) this.orderSPURepo.findById(orderSPUId).orElseThrow(() -> {
            return new ResourceNotFoundException("orderSPU", "id", (long) orderSPUId.intValue());
        });
        OrderDto orderDto = (OrderDto) this.orderServiceImp.getMap().get(orderSPU.getOrderNumber() + "," + orderSPU.getRegel());
        orderDto.setSpu("");
        this.orderServiceImp.updateOrder(orderDto, orderDto.getId(), true);
        this.orderSPUMap.remove(orderSPU.getOrderNumber() + " - " + orderSPU.getRegel());
        this.orderSPURepo.delete(orderSPU);
        return true;
    }

    public OrderSPUDto getOrderSPU(String orderNumber, String regel) {
        if (!this.orderSPUMap.isEmpty() && this.orderSPUMap.containsKey(orderNumber + " - " + regel)) {
            return (OrderSPUDto) this.orderSPUMap.get(orderNumber + " - " + regel);
        } else {
            OrderSPU orderSPU = this.orderSPURepo.findByOrderNumberAndRegel(orderNumber, regel);
            return orderSPU == null ? null : this.spuToDto(orderSPU);
        }
    }

    public OrderSMEDto createOrderSME(OrderSMEDto orderSMEDto) {
        OrderSMEDto orderSMEDtoMapVal = this.getOrderSME(orderSMEDto.getOrderNumber(), orderSMEDto.getRegel());
        if (orderSMEDtoMapVal == null) {
            OrderDto orderDto = (OrderDto) this.orderServiceImp.getMap().get(orderSMEDto.getOrderNumber() + "," + orderSMEDto.getRegel());
            orderDto.setSme("R");
            this.orderServiceImp.updateOrder(orderDto, orderDto.getId(), true);
            OrderSME orderSMESaved = (OrderSME) this.orderSMERepo.save(this.dtoToSME(orderSMEDto));
            this.orderSMEMap.put(orderSMESaved.getOrderNumber() + " - " + orderSMESaved.getRegel(), this.smeToDto(orderSMESaved));
            return this.smeToDto(orderSMESaved);
        } else {
            orderSMEDto.setId(orderSMEDtoMapVal.getId());
            return this.updateOrderSME(orderSMEDto);
        }
    }

    public OrderSMEDto updateOrderSME(OrderSMEDto orderSMEDto) {
        OrderSME orderSMEUpdated = (OrderSME) this.orderSMERepo.save(this.dtoToSME(orderSMEDto));
        this.orderSMEMap.put(orderSMEUpdated.getOrderNumber() + " - " + orderSMEUpdated.getRegel(), orderSMEDto);
        return this.smeToDto(orderSMEUpdated);
    }

    public Boolean deleteOrderSME(Long orderSMEId) {
        OrderSME orderSME = (OrderSME) this.orderSMERepo.findById(orderSMEId).orElseThrow(() -> {
            return new ResourceNotFoundException("orderSme", "id", (long) orderSMEId.intValue());
        });
        Map<String, OrderDto> map = this.orderServiceImp.getMap();
        System.out.println(map);
        OrderDto orderDto = (OrderDto) this.orderServiceImp.getMap().get(orderSME.getOrderNumber() + "," + orderSME.getRegel());
        orderDto.setSme("");
        this.orderServiceImp.updateOrder(orderDto, orderDto.getId(), true);
        this.orderSMEMap.remove(orderSME.getOrderNumber() + " - " + orderSME.getRegel());
        this.orderSMERepo.delete(orderSME);
        return true;
    }

    public OrderSMEDto getOrderSME(String orderNumber, String regel) {
        if (!this.orderSMEMap.isEmpty() && this.orderSMEMap.containsKey(orderNumber + " - " + regel)) {
            return (OrderSMEDto) this.orderSMEMap.get(orderNumber + " - " + regel);
        } else {
            OrderSME orderSME = this.orderSMERepo.findByOrderNumberAndRegel(orderNumber, regel);
            return orderSME == null ? null : this.smeToDto(orderSME);
        }
    }

    public List<OrderSPUDto> getAllSpu() {
        List<OrderSPU> listSpu = this.orderSPURepo.findAll();
        return (List) listSpu.stream().map((spu) -> {
            return this.spuToDto(spu);
        }).collect(Collectors.toList());
    }

    public List<OrderSMEDto> getAllSme() {
        List<OrderSME> listSme = this.orderSMERepo.findAll();
        return (List) listSme.stream().map((sme) -> {
            return this.smeToDto(sme);
        }).collect(Collectors.toList());
    }

    public OrderSME dtoToSME(OrderSMEDto orderSMEDto) {
        OrderSME orderSME = (OrderSME) this.modelMapper.map(orderSMEDto, OrderSME.class);
        return orderSME;
    }

    public OrderSMEDto smeToDto(OrderSME orderSME) {
        OrderSMEDto orderSMEDto = (OrderSMEDto) this.modelMapper.map(orderSME, OrderSMEDto.class);
        return orderSMEDto;
    }

    public OrderSPU dtoToSPU(OrderSPUDto orderSPUDto) {
        OrderSPU orderSPU = (OrderSPU) this.modelMapper.map(orderSPUDto, OrderSPU.class);
        return orderSPU;
    }

    public OrderSPUDto spuToDto(OrderSPU orderSPU) {
        OrderSPUDto orderSPUDto = (OrderSPUDto) this.modelMapper.map(orderSPU, OrderSPUDto.class);
        return orderSPUDto;
    }

    public byte[] generateSMEPdf(String key) {
        String[] parts = key.split(",");
        OrderSMEDto orderSMEDto = this.getOrderSME(parts[0], parts[1]);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] var7;
            try {
                Document document = new Document();
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                document.open();
                this.addSMEHeadingAndAddress(document, "Smederij Order - DM251" + orderSMEDto.getId());
                this.addSMEBloeHeadingAndInfo(writer, document, "", orderSMEDto);
                this.addSMEOptions(writer, document, orderSMEDto);
                this.addSMESections(writer, document, orderSMEDto);
                this.addSMEDatumSections(writer, document);
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

    private void addSMEHeadingAndAddress(Document document, String heading) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 18.0F, 1);
        Font font2 = new Font(FontFamily.COURIER, 25.0F, 1);
        Font font22 = new Font(FontFamily.COURIER, 15.0F, 1);
        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Paragraph paragraph = new Paragraph("DE MOLEN", font2);
        Paragraph paragraphbanden = new Paragraph("    BANDEN", font22);
        paragraph.setAlignment(0);
        paragraphbanden.setAlignment(0);
        cell1.addElement(paragraph);
        cell1.addElement(paragraphbanden);
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

    private void addSMEBloeHeadingAndInfo(PdfWriter writer, Document document, String heading, OrderSMEDto orderSMEDto) throws DocumentException {
        Font font1 = new Font(FontFamily.HELVETICA, 9.2F);
        Font font2 = new Font(FontFamily.HELVETICA, 12.0F);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Barcode128 barcode = new Barcode128();
        barcode.setCode(orderSMEDto.getOrderNumber());
        barcode.setFont((BaseFont) null);
        barcode.setBaseline(0.0F);
        barcode.setBarHeight(15.0F);
        PdfContentByte cb = writer.getDirectContent();
        Image image = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        image.setAlignment(0);
        image.scalePercent(120.0F);
        cell1.addElement(image);
        OrderDto oda = orderServiceImp.getAllOrders().stream()
                .filter(o ->
                        o.getOrderNumber().equals(orderSMEDto.getOrderNumber()) && o.getRegel().equals(orderSMEDto.getRegel())
                ).findFirst().get();
        Paragraph labelParagraph = new Paragraph(String.format("%-9s%-9s", " ", orderSMEDto.getOrderNumber()), font2);
        labelParagraph.setAlignment(0);
        cell1.addElement(labelParagraph);
        cell1.setBorder(0);
        mainTable.addCell(cell1);
        PdfPCell cell2 = new PdfPCell();
//        Paragraph paragraphL1 = new Paragraph(String.format("%-13s%-13s", " Naam Klant:", " " + oda.getCustomerName()), font4);
        Paragraph paragraphL1 = new Paragraph("Naam Klant: " + oda.getCustomerName(), font1);
        Paragraph paragraphL2 = new Paragraph(String.format("%-16s%-16s", "\n Verkoop order:   ", orderSMEDto.getOrderNumber()), font1);
        paragraphL1.setAlignment(0);
        paragraphL2.setAlignment(0);
        cell2.addElement(paragraphL1);
        cell2.addElement(paragraphL2);
        cell2.setBorder(0);
        mainTable.addCell(cell2);
        PdfPCell cell3 = new PdfPCell();
        //Paragraph paragraphR1 = new Paragraph(String.format(""), font4);
        //paragraphR1.setAlignment(1);
        //cell3.addElement(paragraphR1);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        mainTable.setSpacingAfter(45.0F);
        document.add(mainTable);
    }

    private void addSMEOptions(PdfWriter writer, Document document, OrderSMEDto orderSMEDto) throws DocumentException {
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        Font font5 = new Font(FontFamily.HELVETICA, 12.0F);
        if (orderSMEDto.getMerk() == null) {
            orderSMEDto.setMerk("");
        }

        if (orderSMEDto.getModel() == null) {
            orderSMEDto.setModel("");
        }

        if (orderSMEDto.getType() == null) {
            orderSMEDto.setType("");
        }

        if (orderSMEDto.getNaafgat() == null) {
            orderSMEDto.setNaafgat("");
        }

        if (orderSMEDto.getSteek() == null) {
            orderSMEDto.setSteek("");
        }

        if (orderSMEDto.getAantalBoutgat() == null) {
            orderSMEDto.setAantalBoutgat("");
        }

        if (orderSMEDto.getVerdlingBoutgaten() == null) {
            orderSMEDto.setVerdlingBoutgaten("");
        }

        if (orderSMEDto.getDiameter() == null) {
            orderSMEDto.setDiameter("");
        }

        if (orderSMEDto.getTypeBoutgat() == null) {
            orderSMEDto.setTypeBoutgat("");
        }

        if (orderSMEDto.getEt() == null) {
            orderSMEDto.setEt("");
        }

        if (orderSMEDto.getAfstandVV() == null) {
            orderSMEDto.setAfstandVV("");
        }

        if (orderSMEDto.getAfstandVA() == null) {
            orderSMEDto.setAfstandVA("");
        }

        if (orderSMEDto.getDikte() == null) {
            orderSMEDto.setDikte("");
        }

        if (orderSMEDto.getDoorgezet() == null) {
            orderSMEDto.setDoorgezet("");
        }

        if (orderSMEDto.getKoelgaten() == null) {
            orderSMEDto.setKoelgaten("");
        }

        if (orderSMEDto.getVerstevigingsringen() == null) {
            orderSMEDto.setVerstevigingsringen("");
        }

        if (orderSMEDto.getAansluitnippel() == null) {
            orderSMEDto.setAansluitnippel("");
        }

        if (orderSMEDto.getVentielbeschermer() == null) {
            orderSMEDto.setVentielbeschermer("");
        }

        if (orderSMEDto.getOptionVentielbeschermer() == null) {
            orderSMEDto.setOptionVentielbeschermer("");
        }

        OrderDto orders = (OrderDto) this.orderServiceImp.getMap().get(orderSMEDto.getOrderNumber() + "," + orderSMEDto.getRegel());
        if (orders.getOmsumin() == null) {
            orders.setOmsumin("");
        }

        Paragraph labelParagraph2 = new Paragraph("Machine:   " + orderSMEDto.getMerk() + "               Model:   " + orderSMEDto.getModel() + "               Type:   " + orderSMEDto.getType(), font5);
        labelParagraph2.setSpacingAfter(7.0F);
        Paragraph labelParagraph3 = new Paragraph("\n                      Aantal:   " + orders.getAantal() + "\n           Omschrijving:   " + orders.getOmsumin(), font4);
        labelParagraph3.setSpacingAfter(7.0F);
        Paragraph labelParagraph4 = new Paragraph("\n                   Naafgat:   " + orderSMEDto.getNaafgat() + "      mm                                   Steelcirkel:     " + orderSMEDto.getSteek() + "      mm", font4);
        labelParagraph4.setSpacingAfter(7.0F);
        Paragraph labelParagraph5 = new Paragraph("    Aantal Boutgaten:   " + orderSMEDto.getAantalBoutgat() + "                                  Verdeling Boutgaten:     " + orderSMEDto.getVerdlingBoutgaten(), font4);
        labelParagraph5.setSpacingAfter(7.0F);
        Paragraph labelParagraph6 = new Paragraph("    Boutgat Diameter:   " + orderSMEDto.getDiameter() + "      mm                    Uitvoering:     " + orderSMEDto.getTypeBoutgat() + "        Maat Verzinking:    " + orderSMEDto.getMaatVerzinking() + "      mm", font4);
        labelParagraph6.setSpacingAfter(7.0F);
        Paragraph labelParagraph7 = new Paragraph(String.format("%-49s%-49s", " \n                           ET:     " + orderSMEDto.getEt(), "mm"), font4);
        labelParagraph7.setSpacingAfter(7.0F);
        Paragraph labelParagraph8 = new Paragraph(String.format("%-41s%-41s", "    Afstand Voorzijde:     " + orderSMEDto.getAfstandVV(), "mm"), font4);
        labelParagraph8.setSpacingAfter(7.0F);
        Paragraph labelParagraph9 = new Paragraph(String.format("%-39s%-39s", " Afstand Achterzijde:     " + orderSMEDto.getAfstandVA(), "mm"), font4);
        labelParagraph9.setSpacingAfter(7.0F);
        Paragraph labelParagraph10 = new Paragraph(String.format("%-43s%-43s", "              Dikte Schijf:     " + orderSMEDto.getDikte(), "mm                   \t                          Doorgezet:                                  Koelgaten:  "), font4);
        labelParagraph10.setSpacingAfter(5.0F);
        Paragraph labelParagraph11 = new Paragraph(String.format("%-43s%-43s", "                                                                                        Verstevigingsringen:     ", "          Nippel (D/W systeem):  "), font4);
        labelParagraph11.setSpacingAfter(5.0F);
        Paragraph labelParagraph12 = new Paragraph(String.format("%-43s%-43s", "                                                                                          Ventielbeschermer:     ", "                    " + orderSMEDto.getOptionVentielbeschermer()), font4);
        PdfContentByte cb = writer.getDirectContent();
        cb.rectangle(380.0F, 383.0F, 10.0F, 10.0F);
        cb.stroke();
        if (orderSMEDto.getDoorgezet().equals("JA")) {
            cb.moveTo(380.0F, 383.0F);
            cb.lineTo(390.0F, 393.0F);
            cb.moveTo(380.0F, 393.0F);
            cb.lineTo(390.0F, 383.0F);
        }

        PdfContentByte cb2 = writer.getDirectContent();
        cb2.rectangle(380.0F, 363.0F, 10.0F, 10.0F);
        cb2.stroke();
        if (orderSMEDto.getVerstevigingsringen().equals("JA")) {
            cb2.moveTo(380.0F, 363.0F);
            cb2.lineTo(390.0F, 373.0F);
            cb2.moveTo(380.0F, 373.0F);
            cb2.lineTo(390.0F, 363.0F);
        }

        PdfContentByte cb4 = writer.getDirectContent();
        cb4.rectangle(520.0F, 383.0F, 10.0F, 10.0F);
        cb4.stroke();
        if (orderSMEDto.getKoelgaten().equals("JA")) {
            cb4.moveTo(520.0F, 383.0F);
            cb4.lineTo(530.0F, 393.0F);
            cb4.moveTo(520.0F, 393.0F);
            cb4.lineTo(530.0F, 383.0F);
        }

        PdfContentByte cb5 = writer.getDirectContent();
        cb5.rectangle(520.0F, 363.0F, 10.0F, 10.0F);
        cb5.stroke();
        if (orderSMEDto.getAansluitnippel().equals("JA")) {
            cb5.moveTo(520.0F, 363.0F);
            cb5.lineTo(530.0F, 373.0F);
            cb5.moveTo(520.0F, 373.0F);
            cb5.lineTo(530.0F, 363.0F);
        }


        PdfContentByte cb3 = writer.getDirectContent();
        cb3.rectangle(380.0F, 343.0F, 10.0F, 10.0F);
        cb3.stroke();
        if (orderSMEDto.getVentielbeschermer().equals("JA")) {
            cb3.moveTo(380.0F, 343.0F);
            cb3.lineTo(390.0F, 353.0F);
            cb3.moveTo(380.0F, 353.0F);
            cb3.lineTo(390.0F, 343.0F);
        }

        labelParagraph12.setSpacingAfter(15.0F);
        document.add(labelParagraph2);
        document.add(labelParagraph3);
        document.add(labelParagraph4);
        document.add(labelParagraph5);
        document.add(labelParagraph6);
        document.add(labelParagraph7);
        document.add(labelParagraph8);
        document.add(labelParagraph9);
        document.add(labelParagraph10);
        document.add(labelParagraph11);
        document.add(labelParagraph12);
    }

    private void addSMESections(PdfWriter writer, Document document, OrderSMEDto orderSMEDto) throws DocumentException {
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100.0F);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(15);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setBorderWidth(1.0F);
        cell.setMinimumHeight(150.0F);
        Paragraph textParagraph = new Paragraph("               Opmerking:", font4);
        cell.addElement(textParagraph);
        if (orderSMEDto.getOpmerking() != null) {
            Paragraph textParagraph1 = new Paragraph("               " + orderSMEDto.getOpmerking(), font4);
            cell.addElement(textParagraph1);
        }
        table.addCell(cell);
        table.setSpacingAfter(10.0F);
        PdfPTable table2 = new PdfPTable(1);
        table2.setWidthPercentage(100.0F);
        PdfPCell cell2 = new PdfPCell();
        cell2.setBorder(15);
        cell2.setBorderColor(BaseColor.BLACK);
        cell2.setBorderWidth(1.0F);
        cell2.setMinimumHeight(60.0F);
        Paragraph textParagraph2 = new Paragraph("          Flens voorradig : JA/NEE\n          Flens besteld : JA/NEE", font4);
        cell2.addElement(textParagraph2);
        table2.addCell(cell2);
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String formattedDate = sdf.format(currentDate);
        Paragraph printedDatePara = new Paragraph("Printed:    " + formattedDate);
        document.add(table);
        document.add(table2);
        document.add(printedDatePara);
    }

    private void addSMEDatumSections(PdfWriter writer, Document document) throws DocumentException {
        PdfPTable table3 = new PdfPTable(1);
        table3.setWidthPercentage(50.0F);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPCell cell3 = new PdfPCell();
        cell3.setBorder(15);
        cell3.setBorderColor(BaseColor.BLACK);
        cell3.setBorderWidth(1.0F);
        cell3.setMinimumHeight(50.0F);
        Paragraph textParagraph3 = new Paragraph("Datum klaar:", font4);
        cell3.addElement(textParagraph3);
        table3.addCell(cell3);
        table3.setSpacingAfter(10.0F);
        document.add(table3);
    }

    public byte[] generateSPUPdf(String key) {
        String[] parts = key.split(",");
        OrderSPUDto orderSPUDto = this.getOrderSPU(parts[0], parts[1]);
        OrderDto orderDto = (OrderDto) this.orderServiceImp.getMap().get(parts[0] + "," + parts[1]);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] var8;
            try {
                Document document = new Document();
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                document.open();
                this.addSPUHeadingAndAddress(document, "Spuiterij Order");
                this.addSPUBloeHeadingAndInfo(writer, document, "", orderSPUDto, orderDto);
                this.addSPUOptions(writer, document, orderSPUDto);
                this.addSPUSections(writer, document, orderSPUDto, orderDto);
                this.addSPUDatumSections(writer, document);
                System.out.println("Closing Document");
                document.close();
                var8 = outputStream.toByteArray();
            } catch (Throwable var10) {
                try {
                    outputStream.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }

                throw var10;
            }

            outputStream.close();
            return var8;
        } catch (IOException | DocumentException var11) {
            Exception e = var11;
            ((Exception) e).printStackTrace();
            return new byte[0];
        }
    }

    private void addSPUHeadingAndAddress(Document document, String heading) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 18.0F, 1);
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

    private void addSPUBloeHeadingAndInfo(PdfWriter writer, Document document, String heading, OrderSPUDto orderSPUDto, OrderDto orderDto) throws DocumentException {
        Font font2 = new Font(FontFamily.HELVETICA, 12.0F);
        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Barcode128 barcode = new Barcode128();
        barcode.setCode(orderSPUDto.getOrderNumber());
        barcode.setFont((BaseFont) null);
        barcode.setBaseline(0.0F);
        barcode.setBarHeight(15.0F);
        PdfContentByte cb = writer.getDirectContent();
        Image image = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        image.setAlignment(0);
        image.scalePercent(120.0F);
        cell1.addElement(image);
        Paragraph labelParagraph = new Paragraph(String.format("%-9s%-9s", " ", orderSPUDto.getOrderNumber()), font2);
        labelParagraph.setAlignment(0);
        cell1.addElement(labelParagraph);
        cell1.setBorder(0);
        mainTable.addCell(cell1);
        PdfPCell cell3 = new PdfPCell();
        Paragraph paragraphR1 = new Paragraph(String.format(""), font4);
        paragraphR1.setAlignment(1);
        cell3.addElement(paragraphR1);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        PdfPCell cell2 = new PdfPCell();
        Paragraph paragraphL0 = new Paragraph("Aan: ");
        Paragraph paragraphL1 = null;
        Paragraph paragraphL2 = null;
        Paragraph paragraphL3 = null;
        if (orderSPUDto.getAan() != null) {
            if (orderSPUDto.getAan().equals(AanOptions.GEVAK1)) {
                paragraphL1 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.GEVAK1), font3);
                paragraphL2 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.GEVAK2), font4);
                paragraphL3 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.GEVAK3), font4);
            }
            if (orderSPUDto.getAan().equals(AanOptions.KAPPEL1)) {
                paragraphL1 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.KAPPEL1), font3);
                paragraphL2 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.KAPPEL2), font4);
                paragraphL3 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.KAPPEL3), font4);
            }
            if (orderSPUDto.getAan().equals(AanOptions.HURK1)) {
                paragraphL1 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.HURK1), font3);
                paragraphL2 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.HURK2), font4);
                paragraphL3 = new Paragraph(String.format("%-2s%-2s", "", AanOptions.HURK3), font4);
            }
        } else {
            paragraphL1 = new Paragraph(String.format("%-1s%-1s", "", ""), font3);
            paragraphL2 = new Paragraph(String.format("%-2s%-2s", "", ""), font4);
            paragraphL3 = new Paragraph(String.format("%-2s%-2s", "", ""), font4);
        }
        //Paragraph paragraphL4 = new Paragraph(String.format("%-2s%-2s", "", orderDto.getPostCode() + " " + orderDto.getCity()), font4);
        paragraphL1.setAlignment(0);
        paragraphL1.setAlignment(0);
        paragraphL2.setAlignment(0);
        paragraphL3.setAlignment(0);
        //paragraphL4.setAlignment(0);
        cell2.addElement(paragraphL0);
        cell2.addElement(paragraphL1);
        cell2.addElement(paragraphL2);
        cell2.addElement(paragraphL3);
        //cell2.addElement(paragraphL4);
        cell2.setBorder(0);
        mainTable.addCell(cell2);
        mainTable.setSpacingAfter(30.0F);
        document.add(mainTable);
    }

    private void addSPUOptions(PdfWriter writer, Document document, OrderSPUDto orderSPUDto) throws DocumentException {
        Font font5 = new Font(FontFamily.HELVETICA, 13.0F);
        if (orderSPUDto.getPrijscode() == null) {
            orderSPUDto.setPrijscode("");
        }

        if (orderSPUDto.getAfdeling() == null) {
            orderSPUDto.setAfdeling("");
        }

        if (orderSPUDto.getStralen() == null) {
            orderSPUDto.setStralen("");
        }

        if (orderSPUDto.getStralenGedeeltelijk() == null) {
            orderSPUDto.setStralenGedeeltelijk("");
        }

        if (orderSPUDto.getSchooperen() == null) {
            orderSPUDto.setSchooperen("");
        }

        if (orderSPUDto.getPoedercoaten() == null) {
            orderSPUDto.setPoedercoaten("");
        }

        if (orderSPUDto.getKitten() == null) {
            orderSPUDto.setKitten("");
        }

        if (orderSPUDto.getPrimer() == null) {
            orderSPUDto.setPrimer("");
        }

        if (orderSPUDto.getOntlakken() == null) {
            orderSPUDto.setOntlakken("");
        }

        if (orderSPUDto.getKleurOmschrijving() == null) {
            orderSPUDto.setKleurOmschrijving("");
        }

        if (orderSPUDto.getBlankeLak() == null) {
            orderSPUDto.setBlankeLak("");
        }

        OrderDto orders = (OrderDto) this.orderServiceImp.getMap().get(orderSPUDto.getOrderNumber() + "," + orderSPUDto.getRegel());
        if (orders.getOmsumin() == null) {
            orders.setOmsumin("");
        }

        List<WheelColorDto> list = this.wheelServices.getAllWheelColors();
        WheelColorDto wheelColorDto = (WheelColorDto) list.stream().filter((e) -> {
            return e.getColorName().equals(orderSPUDto.getKleurOmschrijving()) && (e.getRed() + "," + e.getGreen() + "," + e.getBlue()).equals(orderSPUDto.getRalCode());
        }).findFirst().orElse(null);
        Paragraph labelParagraph2 = new Paragraph("Produkt            Aantal            Omschrijving", font5);
        Paragraph labelParagraph3 = new Paragraph(orderSPUDto.getProdNumber() + "          " + orders.getAantal() + "         " + orders.getOmsumin(), font5);
        Paragraph labelParagraph4 = new Paragraph("\n                     Stralen: \n Gedeeltelijk Stralen:  \n          Poedercoaten:                                       Prijscode                 Afdeling  \n                       Kitten: \t\t\t\t\t\t\t      \t    \t\t\t\t  \t\t                  \t\t      \t      " + orderSPUDto.getPrijscode() + "                          " + orderSPUDto.getAfdeling() + " \n                      Primer:  \n                Ontlakken: \n                        Kleur:           RAL:  " + wheelColorDto.getId() + "        Naam Kleur:   " + orderSPUDto.getKleurOmschrijving() + " \n          \t      BlankeLak: \n          Verkoop order:  " + orderSPUDto.getOrderNumber() + " \n              Naam Klant:  " + orders.getCustomerName(), font5);


        int lettrCountForRows = 0;
        long letterCount = 0;

        if (orders.getOmsumin().length() > 55) {
            letterCount = orders.getOmsumin().chars()
                    .filter(Character::isLetter)
                    .count();
            if (letterCount >= 18) {
                lettrCountForRows++;
            }
        }
        PdfContentByte cb = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb.rectangle(170.0F, 570.0F, 10.0F, 10.0F);
        } else {
            cb.rectangle(170.0F, 551.0F, 10.0F, 10.0F);

        }
        cb.stroke();

        if (orderSPUDto.getStralen().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb.moveTo(170.0F, 551.0F);
                cb.lineTo(180.0F, 561.0F);
                cb.moveTo(170.0F, 561.0F);
                cb.lineTo(180.0F, 551.0F);
            } else {

                cb.moveTo(170.0F, 570.0F);
                cb.lineTo(180.0F, 580.0F);
                cb.moveTo(170.0F, 580.0F);
                cb.lineTo(180.0F, 570.0F);
            }
        }

        PdfContentByte cb2 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb2.rectangle(170.0F, 550.0F, 10.0F, 10.0F);
        } else {
            cb2.rectangle(170.0F, 531.0F, 10.0F, 10.0F);
        }
        cb2.stroke();
        if (orderSPUDto.getStralenGedeeltelijk().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb2.moveTo(170.0F, 531.0F);
                cb2.lineTo(180.0F, 541.0F);
                cb2.moveTo(170.0F, 541.0F);
                cb2.lineTo(180.0F, 531.0F);
            } else {
                cb2.moveTo(170.0F, 540.0F);
                cb2.lineTo(180.0F, 550.0F);
                cb2.moveTo(170.0F, 550.0F);
                cb2.lineTo(180.0F, 540.0F);
            }
        }

        PdfContentByte cb3 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb3.rectangle(170.0F, 531.0F, 10.0F, 10.0F);
        } else {
            cb3.rectangle(170.0F, 512.0F, 10.0F, 10.0F);
        }
        cb3.stroke();
        if (orderSPUDto.getPoedercoaten().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb3.moveTo(170.0F, 512.0F);
                cb3.lineTo(180.0F, 522.0F);
                cb3.moveTo(170.0F, 522.0F);
                cb3.lineTo(180.0F, 512.0F);
            } else {
                cb3.moveTo(170.0F, 531.0F);
                cb3.lineTo(180.0F, 541.0F);
                cb3.moveTo(170.0F, 541.0F);
                cb3.lineTo(180.0F, 531.0F);
            }
        }

        PdfContentByte cb4 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb4.rectangle(170.0F, 512.0F, 10.0F, 10.0F);
        } else {
            cb4.rectangle(170.0F, 493.0F, 10.0F, 10.0F);
        }
        cb4.stroke();
        if (orderSPUDto.getKitten().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb4.moveTo(170.0F, 493.0F);
                cb4.lineTo(180.0F, 503.0F);
                cb4.moveTo(170.0F, 503.0F);
                cb4.lineTo(180.0F, 493.0F);
            } else {
                cb4.moveTo(170.0F, 512.0F);
                cb4.lineTo(180.0F, 522.0F);
                cb4.moveTo(170.0F, 522.0F);
                cb4.lineTo(180.0F, 512.0F);
            }
        }

        PdfContentByte cb5 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb5.rectangle(170.0F, 493.0F, 10.0F, 10.0F);
        } else {
            cb5.rectangle(170.0F, 474.0F, 10.0F, 10.0F);
        }
        cb5.stroke();
        if (orderSPUDto.getPrimer().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb5.moveTo(170.0F, 474.0F);
                cb5.lineTo(180.0F, 484.0F);
                cb5.moveTo(170.0F, 484.0F);
                cb5.lineTo(180.0F, 474.0F);
            } else {
                cb5.moveTo(170.0F, 493.0F);
                cb5.lineTo(180.0F, 503.0F);
                cb5.moveTo(170.0F, 503.0F);
                cb5.lineTo(180.0F, 493.0F);
            }
        }

        PdfContentByte cb6 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb6.rectangle(170.0F, 474.0F, 10.0F, 10.0F);
        } else {
            cb6.rectangle(170.0F, 455.0F, 10.0F, 10.0F);
        }
        cb6.stroke();
        if (orderSPUDto.getOntlakken().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb6.moveTo(170.0F, 455.0F);
                cb6.lineTo(180.0F, 465.0F);
                cb6.moveTo(170.0F, 465.0F);
                cb6.lineTo(180.0F, 455.0F);
            } else {
                cb6.moveTo(170.0F, 474.0F);
                cb6.lineTo(180.0F, 484.0F);
                cb6.moveTo(170.0F, 484.0F);
                cb6.lineTo(180.0F, 474.0F);
            }
        }

        PdfContentByte cb7 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb7.rectangle(170.0F, 455.0F, 10.0F, 10.0F);
        } else {
            cb7.rectangle(170.0F, 436.0F, 10.0F, 10.0F);
        }
        cb7.stroke();
        if (orderSPUDto.getKleurOmschrijving() != null) {

            if (lettrCountForRows > 0) {
                cb7.moveTo(170.0F, 436.0F);
                cb7.lineTo(180.0F, 446.0F);
                cb7.moveTo(170.0F, 446.0F);
                cb7.lineTo(180.0F, 436.0F);
            } else {
                cb7.moveTo(170.0F, 455.0F);
                cb7.lineTo(180.0F, 465.0F);
                cb7.moveTo(170.0F, 465.0F);
                cb7.lineTo(180.0F, 455.0F);
            }
        }

        PdfContentByte cb8 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb8.rectangle(170.0F, 435.0F, 10.0F, 10.0F);
        } else {
            cb8.rectangle(170.0F, 416.0F, 10.0F, 10.0F);
        }
        cb8.stroke();
        if (orderSPUDto.getBlankeLak().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb8.moveTo(170.0F, 416.0F);
                cb8.lineTo(180.0F, 426.0F);
                cb8.moveTo(170.0F, 426.0F);
                cb8.lineTo(180.0F, 416.0F);
            } else {
                cb8.moveTo(170.0F, 435.0F);
                cb8.lineTo(180.0F, 445.0F);
                cb8.moveTo(170.0F, 445.0F);
                cb8.lineTo(180.0F, 435.0F);
            }
        }

        labelParagraph4.setSpacingAfter(30.0F);
        document.add(labelParagraph2);
        document.add(labelParagraph3);
        document.add(labelParagraph4);
    }

    private void addSPUSections(PdfWriter writer, Document document, OrderSPUDto orderSPUDto, OrderDto orderDto) throws DocumentException {
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100.0F);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(15);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setBorderWidth(1.0F);
        cell.setMinimumHeight(140.0F);
        Paragraph textParagraph = new Paragraph("               Opmerking:", font4);
        cell.addElement(textParagraph);
        if (orderSPUDto.getOpmerking() != null) {
            Paragraph textParagraph1 = new Paragraph("               " + orderSPUDto.getOpmerking(), font4);
            cell.addElement(textParagraph1);
        }
        table.addCell(cell);
        table.setSpacingAfter(10.0F);
        PdfPTable table2 = new PdfPTable(1);
        table2.setWidthPercentage(100.0F);
        PdfPCell cell2 = new PdfPCell();
        cell2.setBorder(15);
        cell2.setBorderColor(BaseColor.BLACK);
        cell2.setBorderWidth(1.0F);
        cell2.setMinimumHeight(80.0F);
        Paragraph textParagraph2 = new Paragraph("               Een bon is voor uw interne administratie. Als de velg(en) klaar is (zijn) dan 1 bon retour geven aan\n                                                                            De Molen Banden B.V.", font4);
        cell2.addElement(textParagraph2);
        table2.addCell(cell2);
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String formattedDate = sdf.format(currentDate);
        Paragraph printedDatePara = new Paragraph("Printed:    " + formattedDate);
        document.add(table);
        document.add(table2);
        document.add(printedDatePara);
    }

    private void addSPUDatumSections(PdfWriter writer, Document document) throws DocumentException {
        PdfPTable table3 = new PdfPTable(1);
        table3.setWidthPercentage(50.0F);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPCell cell3 = new PdfPCell();
        cell3.setBorder(15);
        cell3.setBorderColor(BaseColor.BLACK);
        cell3.setBorderWidth(1.0F);
        cell3.setMinimumHeight(50.0F);
        Paragraph textParagraph3 = new Paragraph("Datum klaar:", font4);
        cell3.addElement(textParagraph3);
        table3.addCell(cell3);
        table3.setSpacingAfter(10.0F);
        document.add(table3);
    }
}
