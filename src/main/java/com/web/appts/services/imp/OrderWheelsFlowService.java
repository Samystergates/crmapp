
package com.web.appts.services.imp;

import com.itextpdf.text.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.web.appts.DTO.*;
import com.web.appts.configurations.JwtTokenHelper;
import com.web.appts.controllers.CheckboxCellEvent;
import com.web.appts.entities.OrderSME;
import com.web.appts.entities.OrderSPU;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.OrderSMERepo;
import com.web.appts.repositories.OrderSPURepo;
import com.web.appts.repositories.PriceCodesRepo;
import com.web.appts.repositories.SpuDepartmentsRepo;
import com.web.appts.services.OrderSMEService;
import com.web.appts.services.OrderSPUService;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.web.appts.utils.AanOptions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

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
    @Autowired
    PriceCodesRepo priceCodesRepo;
    @Autowired
    SpuDepartmentsRepo spuDepartmentsRepo;
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
            orderSMEDto.setForgeNumber(generateNextForgeNumber());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            orderSMEDto.setSlipCreator(authentication.getName());
            OrderSME orderSMESaved = (OrderSME) this.orderSMERepo.save(this.dtoToSME(orderSMEDto));
            this.orderSMEMap.put(orderSMESaved.getOrderNumber() + " - " + orderSMESaved.getRegel(), this.smeToDto(orderSMESaved));
            return this.smeToDto(orderSMESaved);
        } else {
            orderSMEDto.setId(orderSMEDtoMapVal.getId());
            return this.updateOrderSME(orderSMEDto);
        }
    }

    public String generateNextForgeNumber() {
        OrderSME lastEntry = orderSMERepo.findTopByOrderByIdDesc();

        int currentYear = LocalDate.now().getYear();
        int yearSuffix = currentYear % 100;
        int nextSequence = 1;

        if (lastEntry != null) {
            String lastForgeNumber = lastEntry.getForgeNumber();

            if (lastForgeNumber != null && !lastForgeNumber.isEmpty() && lastForgeNumber.length() >= 6 && lastForgeNumber.startsWith("DM")) {
                try {
                    String lastYearStr = lastForgeNumber.substring(2, 4);
                    String lastSeqStr = lastForgeNumber.substring(4);

                    int lastYear = Integer.parseInt(lastYearStr);
                    int lastSeq = Integer.parseInt(lastSeqStr);

                    if (lastYear == yearSuffix) {
                        nextSequence = lastSeq + 1;
                    }
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        return String.format("DM%02d%04d", yearSuffix, nextSequence);
    }


    public OrderSMEDto updateOrderSME(OrderSMEDto orderSMEDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        orderSMEDto.setSlipCreator(authentication.getName());
        OrderSME orderSMEUpdated = (OrderSME) this.orderSMERepo.save(this.dtoToSME(orderSMEDto));
        this.orderSMEMap.put(orderSMEUpdated.getOrderNumber() + " - " + orderSMEUpdated.getRegel(), orderSMEDto);
        return this.smeToDto(orderSMEUpdated);
    }

    public Boolean deleteOrderSME(Long orderSMEId) {
        OrderSME orderSME = (OrderSME) this.orderSMERepo.findById(orderSMEId).orElseThrow(() -> {
            return new ResourceNotFoundException("orderSme", "id", (long) orderSMEId.intValue());
        });
        Map<String, OrderDto> map = this.orderServiceImp.getMap();
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

    public List<OrderSMEDto> getAllSme() {
        List<OrderSME> listSme = this.orderSMERepo.findAll();
        return (List) listSme.stream().map((sme) -> {
            return this.smeToDto(sme);
        }).collect(Collectors.toList());
    }


    public List<OrderSPUDto> getAllSpu() {
        List<OrderSPU> listSpu = this.orderSPURepo.findAll();
        return (List) listSpu.stream().map((spu) -> {
            return this.spuToDto(spu);
        }).collect(Collectors.toList());
    }


    @Override
    public List<PriceCodesDto> getAllPriceCodes() {
        return priceCodesRepo.findAll().stream().map(pc -> modelMapper.map(pc, PriceCodesDto.class)).collect(Collectors.toList());
    }

    @Override
    public List<SpuDepartmentsDto> getAllSpuDepartments() {
        return spuDepartmentsRepo.findAll().stream().map(pc -> modelMapper.map(pc, SpuDepartmentsDto.class)).collect(Collectors.toList());
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
                this.addSMEHeadingAndAddress(document, "Smederij Order - " + orderSMEDto.getForgeNumber());
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

    private void addSMEHeadingAndAddress(Document document, String heading) throws DocumentException, IOException {
        Font font = new Font(FontFamily.HELVETICA, 18.0F, 1);
        Font font2 = new Font(FontFamily.COURIER, 25.0F, 1);
        Font font22 = new Font(FontFamily.COURIER, 15.0F, 1);
        Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
//        PdfPCell cell1 = new PdfPCell();
//        Paragraph paragraph = new Paragraph("DE MOLEN", font2);
//        Paragraph paragraphbanden = new Paragraph("    BANDEN", font22);
//        paragraph.setAlignment(0);
//        paragraphbanden.setAlignment(0);
//        cell1.addElement(paragraph);
//        cell1.addElement(paragraphbanden);
//        cell1.setBorder(0);
//        mainTable.addCell(cell1);

        PdfPCell cell1 = new PdfPCell();
        cell1.setBorder(0);
        cell1.setPaddingTop(12);
        cell1.setPaddingRight(22);
        try (InputStream imageStream = getClass().getClassLoader().getResourceAsStream("images/demolen.jpg")) {
            if (imageStream == null) {
                throw new FileNotFoundException("Image not found: images/demolen.jpg");
            }

            Image logoImage = Image.getInstance(ImageIO.read(imageStream), null);
            logoImage.scaleToFit(150, 150);
            logoImage.setAlignment(Element.ALIGN_CENTER);
            cell1.addElement(logoImage);
        }

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

        Paragraph paragraphL2 = new Paragraph("Ingevoerd door: " + orderSMEDto.getSlipCreator(), font1);
        Paragraph paragraphL3 = new Paragraph(String.format("%-16s%-16s", "Verkoop order:   ", orderSMEDto.getOrderNumber() + " - " + orderSMEDto.getRegel()), font1);
        paragraphL1.setAlignment(0);
        paragraphL2.setAlignment(0);
        paragraphL3.setAlignment(0);

        cell2.addElement(paragraphL1);
        cell2.addElement(paragraphL2);
        cell2.addElement(paragraphL3);
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

//    private void addSMEOptions(PdfWriter writer, Document document, OrderSMEDto orderSMEDto) throws DocumentException {
//        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
//        Font font5 = new Font(FontFamily.HELVETICA, 12.0F);
//        if (orderSMEDto.getMerk() == null) {
//            orderSMEDto.setMerk("");
//        }
//
//        if (orderSMEDto.getModel() == null) {
//            orderSMEDto.setModel("");
//        }
//
//        if (orderSMEDto.getType() == null) {
//            orderSMEDto.setType("");
//        }
//
//        if (orderSMEDto.getNaafgat() == null) {
//            orderSMEDto.setNaafgat("");
//        }
//
//        if (orderSMEDto.getSteek() == null) {
//            orderSMEDto.setSteek("");
//        }
//
//        if (orderSMEDto.getAantalBoutgat() == null) {
//            orderSMEDto.setAantalBoutgat("");
//        }
//
//        if (orderSMEDto.getVerdlingBoutgaten() == null) {
//            orderSMEDto.setVerdlingBoutgaten("");
//        }
//
//        if (orderSMEDto.getDiameter() == null) {
//            orderSMEDto.setDiameter("");
//        }
//
//        if (orderSMEDto.getTypeBoutgat() == null) {
//            orderSMEDto.setTypeBoutgat("");
//        }
//
//        if (orderSMEDto.getEt() == null) {
//            orderSMEDto.setEt("");
//        }
//
//        if (orderSMEDto.getAfstandVV() == null) {
//            orderSMEDto.setAfstandVV("");
//        }
//
//        if (orderSMEDto.getAfstandVA() == null) {
//            orderSMEDto.setAfstandVA("");
//        }
//
//        if (orderSMEDto.getDikte() == null) {
//            orderSMEDto.setDikte("");
//        }
//
//        if (orderSMEDto.getDoorgezet() == null) {
//            orderSMEDto.setDoorgezet("");
//        }
//
//        if (orderSMEDto.getKoelgaten() == null) {
//            orderSMEDto.setKoelgaten("");
//        }
//
//        if (orderSMEDto.getVerstevigingsringen() == null) {
//            orderSMEDto.setVerstevigingsringen("");
//        }
//
//        if (orderSMEDto.getAansluitnippel() == null) {
//            orderSMEDto.setAansluitnippel("");
//        }
//
//        if (orderSMEDto.getVentielbeschermer() == null) {
//            orderSMEDto.setVentielbeschermer("");
//        }
//
//        if (orderSMEDto.getOptionVentielbeschermer() == null) {
//            orderSMEDto.setOptionVentielbeschermer("");
//        }
//
//        OrderDto orders = (OrderDto) this.orderServiceImp.getMap().get(orderSMEDto.getOrderNumber() + "," + orderSMEDto.getRegel());
//        if (orders.getOmsumin() == null) {
//            orders.setOmsumin("");
//        }
//
//
//        Paragraph labelParagraph2 = new Paragraph("Machine:   " + orderSMEDto.getMerk() + "               Model:   " + orderSMEDto.getModel() + "               Type:   " + orderSMEDto.getType(), font5);
//        labelParagraph2.setSpacingAfter(7.0F);
//        Paragraph labelParagraph3 = new Paragraph("\n                      Aantal:   " + getFormattedAantal(orders.getAantal()) + "\n           Omschrijving:   " + orders.getOmsumin(), font4);
//        labelParagraph3.setSpacingAfter(7.0F);
//        Paragraph labelParagraph4 = new Paragraph("\n                   Naafgat:   " + orderSMEDto.getNaafgat() + "      mm                                   Steelcirkel:     " + orderSMEDto.getSteek() + "      mm", font4);
//        labelParagraph4.setSpacingAfter(7.0F);
//        Paragraph labelParagraph5 = new Paragraph("    Aantal Boutgaten:   " + orderSMEDto.getAantalBoutgat() + "                                  Verdeling Boutgaten:     " + orderSMEDto.getVerdlingBoutgaten(), font4);
//        labelParagraph5.setSpacingAfter(7.0F);
//        Paragraph labelParagraph6 = new Paragraph("    Boutgat Diameter:   " + orderSMEDto.getDiameter() + "      mm                    Uitvoering:     " + orderSMEDto.getTypeBoutgat() + "        Maat Verzinking:    " + orderSMEDto.getMaatVerzinking() + "      mm", font4);
//        labelParagraph6.setSpacingAfter(7.0F);
//        Paragraph labelParagraph7 = new Paragraph(String.format("%-49s%-49s", " \n                           ET:     " + orderSMEDto.getEt(), "mm"), font4);
//        labelParagraph7.setSpacingAfter(7.0F);
//        Paragraph labelParagraph8 = new Paragraph(String.format("%-41s%-41s", "    Afstand Voorzijde:     " + orderSMEDto.getAfstandVV(), "mm"), font4);
//        labelParagraph8.setSpacingAfter(7.0F);
//        Paragraph labelParagraph9 = new Paragraph(String.format("%-39s%-39s", " Afstand Achterzijde:     " + orderSMEDto.getAfstandVA(), "mm"), font4);
//        labelParagraph9.setSpacingAfter(7.0F);
//
//        PdfPTable table = new PdfPTable(4);
//        table.setWidthPercentage(60);
//        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        table.setWidths(new float[]{6, 2, 6, 2});
//
//        table.addCell(createTextCell("Doorgezet:"));
//        table.addCell(createCheckboxCell(orderSMEDto.getDoorgezet().equals("JA")));
//
//        table.addCell(createTextCell("Koelgaten:"));
//        table.addCell(createCheckboxCell(orderSMEDto.getKoelgaten().equals("JA")));
//
//        table.addCell(createTextCell("Verstevigingsringen:"));
//        table.addCell(createCheckboxCell(orderSMEDto.getVerstevigingsringen().equals("JA")));
//
//        table.addCell(createTextCell("Nippel (D/W systeem):"));
//        table.addCell(createCheckboxCell(orderSMEDto.getAansluitnippel().equals("JA")));
//
//        table.addCell(createTextCell("Ventielbeschermer:"));
//        table.addCell(createCheckboxCell(orderSMEDto.getVentielbeschermer().equals("JA")));
//
//        PdfPCell defaultCell = new PdfPCell(new Phrase());
//        defaultCell.setBorder(Rectangle.NO_BORDER);
//
//        table.addCell(createTextCell(orderSMEDto.getOptionVentielbeschermer()));
//        table.addCell(defaultCell);
//
//        document.add(labelParagraph2);
//        document.add(labelParagraph3);
//        document.add(labelParagraph4);
//        document.add(labelParagraph5);
//        document.add(labelParagraph6);
//        document.add(labelParagraph7);
//        document.add(labelParagraph8);
//        document.add(labelParagraph9);
//
//        table.setSpacingAfter(15.0F);
//        document.add(table);
//
//    }
//
//    private PdfPCell createTextCell(String text) {
//        Font smallFont = new Font(Font.FontFamily.HELVETICA, 11); // Set font size
//        PdfPCell cell = new PdfPCell(new Phrase(text, smallFont));
//        cell.setBorder(Rectangle.NO_BORDER);
//        cell.setHorizontalAlignment(Element.ALIGN_RIGHT); // Align text to the right
//        return cell;
//    }


    private void addSMEOptions(PdfWriter writer, Document document, OrderSMEDto orderSMEDto) throws DocumentException {
        Font font4 = new Font(Font.FontFamily.HELVETICA, 10.0F);
        Font font5 = new Font(Font.FontFamily.HELVETICA, 12.0F);

        if (orderSMEDto.getMerk() == null) orderSMEDto.setMerk("");
        if (orderSMEDto.getModel() == null) orderSMEDto.setModel("");
        if (orderSMEDto.getType() == null) orderSMEDto.setType("");
        if (orderSMEDto.getNaafgat() == null) orderSMEDto.setNaafgat("");
        if (orderSMEDto.getSteek() == null) orderSMEDto.setSteek("");
        if (orderSMEDto.getAantalBoutgat() == null) orderSMEDto.setAantalBoutgat("");
        if (orderSMEDto.getVerdlingBoutgaten() == null) orderSMEDto.setVerdlingBoutgaten("");
        if (orderSMEDto.getDiameter() == null) orderSMEDto.setDiameter("");
        if (orderSMEDto.getTypeBoutgat() == null) orderSMEDto.setTypeBoutgat("");
        if (orderSMEDto.getEt() == null) orderSMEDto.setEt("");
        if (orderSMEDto.getAfstandVV() == null) orderSMEDto.setAfstandVV("");
        if (orderSMEDto.getAfstandVA() == null) orderSMEDto.setAfstandVA("");
        if (orderSMEDto.getDikte() == null) orderSMEDto.setDikte("");
        if (orderSMEDto.getDoorgezet() == null) orderSMEDto.setDoorgezet("");
        if (orderSMEDto.getKoelgaten() == null) orderSMEDto.setKoelgaten("");
        if (orderSMEDto.getVerstevigingsringen() == null) orderSMEDto.setVerstevigingsringen("");
        if (orderSMEDto.getAansluitnippel() == null) orderSMEDto.setAansluitnippel("");
        if (orderSMEDto.getVentielbeschermer() == null) orderSMEDto.setVentielbeschermer("");
        if (orderSMEDto.getOptionVentielbeschermer() == null) orderSMEDto.setOptionVentielbeschermer("");

        OrderDto orders = (OrderDto) this.orderServiceImp.getMap().get(orderSMEDto.getOrderNumber() + "," + orderSMEDto.getRegel());
        if (orders.getOmsumin() == null) orders.setOmsumin("");

        Paragraph labelParagraph2 = new Paragraph("Machine:   " + orderSMEDto.getMerk() + "               Model:   " + orderSMEDto.getModel() + "               Type:   " + orderSMEDto.getType(), font5);
        labelParagraph2.setSpacingAfter(14.0F);
        document.add(labelParagraph2);

        // ---------------- Table 1: Aantal & Omschrijving ----------------
        PdfPTable table1 = new PdfPTable(2);
        table1.setWidthPercentage(100);
        table1.setWidths(new float[]{1, 4});
        table1.addCell(createRightAlignedTextCell("Aantal:"));
        table1.addCell(createLeftAlignedTextCell((getFormattedAantal(orders.getAantal()))));
        table1.addCell(createRightAlignedTextCell("Omschrijving:"));
        table1.addCell(createLeftAlignedTextCell((orders.getOmsumin())));
        table1.setSpacingAfter(14f);
        document.add(table1);

        // ---------------- Table 2: Naafgat, Steelcirkel, Aantal Boutgaten, Verdeling ----------------
        PdfPTable table2 = new PdfPTable(6);
        table2.setWidthPercentage(100);
        table2.setWidths(new float[]{4, 3, 2, 5, 3, 3});
        table2.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        // Row 1
        table2.addCell(createRightAlignedTextCell("Naafgat:"));
        table2.addCell(createRightAlignedTextCell(orderSMEDto.getNaafgat()));
        table2.addCell(createTextCell("mm"));
        table2.addCell(createRightAlignedTextCell("Steelcirkel:"));
        table2.addCell(createTextCell(orderSMEDto.getSteek()));
        table2.addCell(createTextCell("mm"));
        // Row 2
        table2.addCell(createRightAlignedTextCell("Aantal Boutgaten:"));
        table2.addCell(createRightAlignedTextCell(orderSMEDto.getAantalBoutgat()));
        table2.addCell(createLeftAlignedTextCell(""));
        table2.addCell(createRightAlignedTextCell("Verdeling Boutgaten:"));
        table2.addCell(createTextCell(orderSMEDto.getVerdlingBoutgaten()));
        table2.addCell(createTextCell(""));
        document.add(table2);

        // ---------------- Table 3: Boutgat Diameter, Uitvoering, Maat Verzinking ----------------
        PdfPTable table3 = new PdfPTable(8);
        table3.setWidthPercentage(100);
        table3.setWidths(new float[]{7, 3, 3, 4, 2, 5, 2, 3});
        table3.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table3.addCell(createLeftAlignedTextCell("     Boutgat Diameter:"));
        table3.addCell(custRightAlignedTextCell(orderSMEDto.getDiameter()));
        table3.addCell(customeMM("mm"));
        table3.addCell(custRightAlignedTextCell("Uitvoering:"));
        table3.addCell(createTextCell(orderSMEDto.getTypeBoutgat()));
        table3.addCell(createTextCell("Maat Verzinking:"));
        table3.addCell(createTextCell(orderSMEDto.getMaatVerzinking()));
        table3.addCell(createLeftAlignedTextCell("  mm"));
        table3.setSpacingAfter(14f);
        document.add(table3);

        // ---------------- Table 4: ET, Afstand Voorzijde, Afstand Achterzijde ----------------
        PdfPTable table4 = new PdfPTable(6);
        table4.setWidthPercentage(100);
        table4.setWidths(new float[]{4, 3, 2, 5, 3, 3});
        table4.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        // Row 1
        table4.addCell(createRightAlignedTextCell("ET:"));
        table4.addCell(createRightAlignedTextCell(orderSMEDto.getEt()));
        table4.addCell(createTextCell("mm"));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));

        // Row 2
        table4.addCell(createRightAlignedTextCell("Afstand Voorzijde:"));
        table4.addCell(createRightAlignedTextCell(orderSMEDto.getAfstandVV()));
        table4.addCell(createTextCell("mm"));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));

        // Row 3
        table4.addCell(createRightAlignedTextCell("Afstand Achterzijde:"));
        table4.addCell(createRightAlignedTextCell(orderSMEDto.getAfstandVA()));
        table4.addCell(createTextCell("mm"));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));
        table4.addCell(createTextCell(""));

        document.add(table4);

        // ---------------- Existing Checkboxes Table ----------------
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{6, 2, 6, 2});
        table.addCell(createTextCell("Doorgezet:"));
        table.addCell(createCheckboxCell(orderSMEDto.getDoorgezet().equals("JA")));
        table.addCell(createTextCell("Koelgaten:"));
        table.addCell(createCheckboxCell(orderSMEDto.getKoelgaten().equals("JA")));
        table.addCell(createTextCell("Verstevigingsringen:"));
        table.addCell(createCheckboxCell(orderSMEDto.getVerstevigingsringen().equals("JA")));
        table.addCell(createTextCell("Nippel (D/W systeem):"));
        table.addCell(createCheckboxCell(orderSMEDto.getAansluitnippel().equals("JA")));
        table.addCell(createTextCell("Ventielbeschermer:"));
        table.addCell(createCheckboxCell(orderSMEDto.getVentielbeschermer().equals("JA")));
        PdfPCell defaultCell = new PdfPCell(new Phrase());
        defaultCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(createTextCell(orderSMEDto.getOptionVentielbeschermer()));
        table.addCell(defaultCell);
        table.setSpacingAfter(15.0F);
        document.add(table);
    }

    private PdfPCell createTextCell(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell createLeftAlignedTextCell(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell createRightAlignedTextCell(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPaddingRight(8f);
        cell.setPaddingBottom(12f);
        return cell;
    }


    private PdfPCell custRightAlignedTextCell(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPaddingRight(5f);
        cell.setPaddingBottom(12f);
        return cell;
    }

    private PdfPCell customeMM(String text) {
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, smallFont));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    public String getFormattedAantal(String aantal) {
        if (aantal != null && aantal.endsWith(".000")) {
            return aantal.substring(0, aantal.length() - 4);
        }
        return aantal;
    }

    private PdfPCell createCheckboxCell(boolean isChecked) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(12f);
        cell.setCellEvent(new CheckboxCellEvent(isChecked));
        return cell;
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDate = sdf.format(currentDate);
        Paragraph printedDatePara = new Paragraph("Afgedrukt op:    " + formattedDate);
        printedDatePara.setSpacingAfter(12f);
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
        orderServiceImp.getMap().clear();

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
        barcode.setCode(orderSPUDto.getOrderNumber() + "-" + orderSPUDto.getRegel());
        barcode.setFont((BaseFont) null);
        barcode.setBaseline(0.0F);
        barcode.setBarHeight(15.0F);
        PdfContentByte cb = writer.getDirectContent();
        Image image = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        image.setAlignment(0);
        image.scalePercent(120.0F);
        cell1.addElement(image);
        Paragraph labelParagraph = new Paragraph(String.format("%-9s%-9s", "", "* " + orderSPUDto.getOrderNumber() + "-" + orderSPUDto.getRegel() + " *"), font2);
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

        if (orderSPUDto.getAflakken() == null) {
            orderSPUDto.setAflakken("");
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

        PdfPTable productTable = new PdfPTable(4);
        productTable.setWidthPercentage(100f);
        productTable.setWidths(new float[]{13f, 9f, 63f, 15f}); // Third column is wide

        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 10f);
        Font fontValue = new Font(Font.FontFamily.HELVETICA, 10f);

// Header row
        productTable.addCell(createHeaderCell("Product", fontLabel));
        productTable.addCell(createHeaderCell("Aantal", fontLabel));
        productTable.addCell(createHeaderCell("Omschrijving", fontLabel));
        productTable.addCell(createHeaderCell("SM Nummer", fontLabel));

// Data row
        DecimalFormat df = new DecimalFormat("0.00");
        String aantalRaw = orders.getAantal();
        double aantalValue = Double.parseDouble(aantalRaw);
        String formattedAantal = df.format(aantalValue);
        productTable.addCell(createDataCell(orderSPUDto.getProdNumber(), fontValue));
        productTable.addCell(createDataCell(formattedAantal, fontValue));
        productTable.addCell(createDataCell(orders.getOmsumin(), fontValue));
        String sm = "";
        OrderSMEDto orderSMEDto = getOrderSME(orderSPUDto.getOrderNumber(), orderSPUDto.getRegel());
        if (orderSMEDto != null && orderSMEDto.getForgeNumber() != null) {
            sm = orderSMEDto.getForgeNumber();
        }
        productTable.addCell(createDataCell(sm, fontValue));

        String spacingForAfdeling = "";
        if (orderSPUDto.getPrijscode().equals("")) {
            spacingForAfdeling = "        ";
        }

        String toDisplay = "";
        if (orderSPUDto.getPoedercoaten() != null && orderSPUDto.getPoedercoaten().equals("JA")) {
            toDisplay = "Poedercoaten";
        }
        if (orderSPUDto.getNatLakken() != null && orderSPUDto.getNatLakken().equals("JA")) {
            toDisplay = "Nat Lakken";

        }
        Paragraph labelParagraph4 = new Paragraph("\n                     " + toDisplay + " \n                     Stralen: \n Gedeeltelijk Stralen:  \n                  Aflakken:                                       Prijscode                 Afdeling  \n                       Kitten: \t\t\t\t\t\t\t      \t    \t\t\t\t  \t\t                  \t\t      \t      " + orderSPUDto.getPrijscode() + "                  " + spacingForAfdeling + "" + orderSPUDto.getAfdeling() + " \n                      Primer:  \n                Ontlakken: \n                        Kleur:           RAL:  " + wheelColorDto.getId() + "        Naam Kleur:   " + orderSPUDto.getKleurOmschrijving() + " \n          \t      BlankeLak: \n          Verkoop order:  " + orderSPUDto.getOrderNumber() + " \n              Naam Klant:  " + orders.getCustomerName(), font5);


        int lettrCountForRows = 0;

        Rectangle pageSize = document.getPageSize();
        float contentWidth = pageSize.getWidth() - document.leftMargin() - document.rightMargin();

        float totalParts = 13f + 9f + 63f + 15f;
        float omschrijvingWidth = contentWidth * (63f / totalParts);

        String text = orders.getOmsumin();
        int breakIndex = 0;
        try {
            breakIndex = getLineBreakIndex(text, FontFamily.HELVETICA, 10f, omschrijvingWidth);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("bi: "+breakIndex);
        if(text.substring(breakIndex) != null && !text.substring(breakIndex).equals("")){
            ++lettrCountForRows;
        }
        System.out.println("Column width points: " + omschrijvingWidth);
        System.out.println("Line will break at index: " + breakIndex);
        System.out.println("First line: " + text.substring(0, breakIndex));
        System.out.println("Remaining: " + text.substring(breakIndex));



//        PdfContentByte cb = writer.getDirectContent();
//        if (lettrCountForRows <= 0) {
//            cb.rectangle(170.0F, 570.0F, 10.0F, 10.0F);
//        } else {
//            cb.rectangle(170.0F, 560.0F, 10.0F, 10.0F);
//
//        }
//        cb.stroke();
//
//        if (orderSPUDto.getStralen().equals("JA")) {
//
//            if (lettrCountForRows > 0) {
//                cb.moveTo(170.0F, 560.0F);
//                cb.lineTo(180.0F, 570.0F);
//                cb.moveTo(170.0F, 570.0F);
//                cb.lineTo(180.0F, 560.0F);
//            } else {
//
//                cb.moveTo(170.0F, 570.0F);
//                cb.lineTo(180.0F, 580.0F);
//                cb.moveTo(170.0F, 580.0F);
//                cb.lineTo(180.0F, 570.0F);
//            }
//        }

        PdfContentByte cb2 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb2.rectangle(170.0F, 550.0F, 10.0F, 10.0F);
        } else {
            cb2.rectangle(170.0F, 540.0F, 10.0F, 10.0F);
        }
        cb2.stroke();
        if (orderSPUDto.getStralen().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb2.moveTo(170.0F, 540.0F);
                cb2.lineTo(180.0F, 550.0F);
                cb2.moveTo(170.0F, 540.0F);
                cb2.lineTo(180.0F, 550.0F);
            } else {
                cb2.moveTo(170.0F, 550.0F);
                cb2.lineTo(180.0F, 560.0F);
                cb2.moveTo(170.0F, 560.0F);
                cb2.lineTo(180.0F, 550.0F);
            }
        }


//        PdfContentByte cb3 = writer.getDirectContent();
//        if (lettrCountForRows <= 0) {
//            cb3.rectangle(170.0F, 531.0F, 10.0F, 10.0F);
//        } else {
//            cb3.rectangle(170.0F, 521.0F, 10.0F, 10.0F);
//        }
//        cb3.stroke();
//        if (orderSPUDto.getPoedercoaten().equals("JA")) {
//
//            if (lettrCountForRows > 0) {
//                cb3.moveTo(170.0F, 521.0F);
//                cb3.lineTo(180.0F, 531.0F);
//                cb3.moveTo(170.0F, 531.0F);
//                cb3.lineTo(180.0F, 521.0F);
//            } else {
//                cb3.moveTo(170.0F, 531.0F);
//                cb3.lineTo(180.0F, 541.0F);
//                cb3.moveTo(170.0F, 541.0F);
//                cb3.lineTo(180.0F, 531.0F);
//            }
//        }

//        PdfContentByte cb9 = writer.getDirectContent();
//        if (lettrCountForRows <= 0) {
//            cb9.rectangle(170.0F, 416.0F, 10.0F, 10.0F);
//        } else {
//            cb9.rectangle(170.0F, 406.0F, 10.0F, 10.0F);
//        }
//        cb9.stroke();
//        if (orderSPUDto.getAflakken().equals("JA")) {
//
//            if (lettrCountForRows > 0) {
//                cb9.moveTo(170.0F, 406.0F);
//                cb9.lineTo(180.0F, 416.0F);
//                cb9.moveTo(170.0F, 416.0F);
//                cb9.lineTo(180.0F, 406.0F);
//            } else {
//                cb9.moveTo(170.0F, 416.0F);
//                cb9.lineTo(180.0F, 426.0F);
//                cb9.moveTo(170.0F, 426.0F);
//                cb9.lineTo(180.0F, 416.0F);
//            }
//        }

        PdfContentByte cb3 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb3.rectangle(170.0F, 531.0F, 10.0F, 10.0F);
        } else {
            cb3.rectangle(170.0F, 521.0F, 10.0F, 10.0F);
        }
        cb3.stroke();
        if (orderSPUDto.getStralenGedeeltelijk().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb3.moveTo(170.0F, 521.0F);
                cb3.lineTo(180.0F, 531.0F);
                cb3.moveTo(170.0F, 531.0F);
                cb3.lineTo(180.0F, 521.0F);
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
            cb4.rectangle(170.0F, 502.0F, 10.0F, 10.0F);
        }
        cb4.stroke();
        if (orderSPUDto.getAflakken().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb4.moveTo(170.0F, 502.0F);
                cb4.lineTo(180.0F, 512.0F);
                cb4.moveTo(170.0F, 512.0F);
                cb4.lineTo(180.0F, 502.0F);
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
            cb5.rectangle(170.0F, 483.0F, 10.0F, 10.0F);
        }
        cb5.stroke();
        if (orderSPUDto.getKitten().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb5.moveTo(170.0F, 483.0F);
                cb5.lineTo(180.0F, 493.0F);
                cb5.moveTo(170.0F, 493.0F);
                cb5.lineTo(180.0F, 483.0F);
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
            cb6.rectangle(170.0F, 464.0F, 10.0F, 10.0F);
        }
        cb6.stroke();
        if (orderSPUDto.getPrimer().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb6.moveTo(170.0F, 464.0F);
                cb6.lineTo(180.0F, 474.0F);
                cb6.moveTo(170.0F, 474.0F);
                cb6.lineTo(180.0F, 464.0F);
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
            cb7.rectangle(170.0F, 445.0F, 10.0F, 10.0F);
        }
        cb7.stroke();
        if (orderSPUDto.getOntlakken().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb7.moveTo(170.0F, 445.0F);
                cb7.lineTo(180.0F, 455.0F);
                cb7.moveTo(170.0F, 455.0F);
                cb7.lineTo(180.0F, 445.0F);
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
            cb8.rectangle(170.0F, 425.0F, 10.0F, 10.0F);
        }
        cb8.stroke();
        if (orderSPUDto.getKleurOmschrijving() != null) {

            if (lettrCountForRows > 0) {
                cb8.moveTo(170.0F, 425.0F);
                cb8.lineTo(180.0F, 435.0F);
                cb8.moveTo(170.0F, 435.0F);
                cb8.lineTo(180.0F, 425.0F);
            } else {
                cb8.moveTo(170.0F, 435.0F);
                cb8.lineTo(180.0F, 445.0F);
                cb8.moveTo(170.0F, 445.0F);
                cb8.lineTo(180.0F, 435.0F);
            }
        }
        PdfContentByte cb9 = writer.getDirectContent();
        if (lettrCountForRows <= 0) {
            cb9.rectangle(170.0F, 416.0F, 10.0F, 10.0F);
        } else {
            cb9.rectangle(170.0F, 406.0F, 10.0F, 10.0F);
        }
        cb9.stroke();
        if (orderSPUDto.getBlankeLak().equals("JA")) {

            if (lettrCountForRows > 0) {
                cb9.moveTo(170.0F, 406.0F);
                cb9.lineTo(180.0F, 416.0F);
                cb9.moveTo(170.0F, 416.0F);
                cb9.lineTo(180.0F, 406.0F);
            } else {
                cb9.moveTo(170.0F, 416.0F);
                cb9.lineTo(180.0F, 426.0F);
                cb9.moveTo(170.0F, 426.0F);
                cb9.lineTo(180.0F, 416.0F);
            }
        }
//        PdfContentByte cb10 = writer.getDirectContent();
//        if (lettrCountForRows <= 0) {
//            cb10.rectangle(170.0F, 397.0F, 10.0F, 10.0F);
//        } else {
//            cb10.rectangle(170.0F, 387.0F, 10.0F, 10.0F);
//        }
//        cb10.stroke();
//        if (orderSPUDto.getBlankeLak().equals("JA")) {
//
//            if (lettrCountForRows > 0) {
//                cb10.moveTo(170.0F, 387.0F);
//                cb10.lineTo(180.0F, 397.0F);
//                cb10.moveTo(170.0F, 397.0F);
//                cb10.lineTo(180.0F, 387.0F);
//            } else {
//                cb10.moveTo(170.0F, 397.0F);
//                cb10.lineTo(180.0F, 407.0F);
//                cb10.moveTo(170.0F, 407.0F);
//                cb10.lineTo(180.0F, 397.0F);
//            }
//        }

        labelParagraph4.setSpacingAfter(30.0F);
        document.add(productTable);
//        document.add(labelParagraph3);
        document.add(labelParagraph4);
    }


    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
//        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(10f);
        return cell;
    }

//    public static int getLineBreakIndex(String text, Font.FontFamily fontFamily, float fontSize, float maxWidthPoints) throws Exception {
//        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
//        float accumulatedWidth = 0f;
//
//        for (int i = 0; i < text.length(); i++) {
//            char ch = text.charAt(i);
//            float charWidth = baseFont.getWidthPoint(String.valueOf(ch), fontSize);
//            if (accumulatedWidth + charWidth > maxWidthPoints) {
//                return i; // Break here
//            }
//            accumulatedWidth += charWidth;
//        }
//        return text.length(); // No break needed
//    }



//    public static int getLineBreakIndex(String text, Font.FontFamily fontFamily, float fontSize, float maxWidthPoints) throws Exception {
//        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
//
//        float accumulatedWidth = 0f;
//        int lastSpaceIndex = -1;
//        float tolerance = 0.0f; // we now handle half-space logic separately
//
//        for (int i = 0; i < text.length(); i++) {
//            char ch = text.charAt(i);
//            float charWidth = baseFont.getWidthPoint(String.valueOf(ch), fontSize);
//
//            // If adding this char exceeds max width, break
//            if (accumulatedWidth + charWidth > maxWidthPoints - tolerance) {
//                return (lastSpaceIndex != -1) ? lastSpaceIndex : i;
//            }
//
//            accumulatedWidth += charWidth;
//
//            // Handle space: look ahead to see if next word will fit
//            if (Character.isWhitespace(ch)) {
//                lastSpaceIndex = i;
//
//                // Look ahead to next word
//                int j = i + 1;
//                float nextWordWidth = 0f;
//                while (j < text.length() && !Character.isWhitespace(text.charAt(j))) {
//                    nextWordWidth += baseFont.getWidthPoint(String.valueOf(text.charAt(j)), fontSize);
//                    j++;
//                }
//
//                // If space + half of it + next word won't fit, break here
//                if (accumulatedWidth + nextWordWidth > maxWidthPoints - (charWidth / 2f)) {
//                    return lastSpaceIndex;
//                }
//            }
//        }
//        return text.length();
//    }


//    public static int getLineBreakIndex(String text, Font.FontFamily fontFamily, float fontSize, float maxWidthPoints) throws Exception {
//        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
//
//        float accumulatedWidth = 0f;
//        int lastSpaceIndex = -1;
//        float tolerance = 0.0f; // keeping zero here, we handle it via charWidth adjustment
//
//        for (int i = 0; i < text.length(); i++) {
//            char ch = text.charAt(i);
//            float charWidth = baseFont.getWidthPoint(String.valueOf(ch), fontSize);
//
//            // If adding this char exceeds max width, break
//            if (accumulatedWidth + charWidth > maxWidthPoints - tolerance) {
//                return (lastSpaceIndex != -1) ? lastSpaceIndex : i;
//            }
//
//            accumulatedWidth += charWidth;
//
//            // Handle space: look ahead to see if next word will fit
//            if (Character.isWhitespace(ch)) {
//                lastSpaceIndex = i;
//
//                // Look ahead to next word
//                int j = i + 1;
//                float nextWordWidth = 0f;
//                while (j < text.length() && !Character.isWhitespace(text.charAt(j))) {
//                    nextWordWidth += baseFont.getWidthPoint(String.valueOf(text.charAt(j)), fontSize);
//                    j++;
//                }
//
//                // If space + full of it + next word won't fit, break here
//                if (accumulatedWidth + nextWordWidth > maxWidthPoints - charWidth) {
//                    return lastSpaceIndex;
//                }
//            }
//        }
//        return text.length();
//    }

    public static int getLineBreakIndex(String text, Font.FontFamily fontFamily, float fontSize, float maxWidthPoints) throws Exception {
        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        float accumulatedWidth = 0f;
        int lastSpaceIndex = -1;

        // Minimal change: use one space width as an extra safety margin.
        float spaceWidth = baseFont.getWidthPoint("  ", fontSize);
        // You can increase this multiplier (e.g. 1.2f) if you still see misses.
        float extraMargin = spaceWidth * 1.0f;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            float charWidth = baseFont.getWidthPoint(String.valueOf(ch), fontSize);

            // Check against a slightly narrower threshold (subtract extraMargin)
            if (accumulatedWidth + charWidth > maxWidthPoints - extraMargin) {
                return (lastSpaceIndex != -1) ? lastSpaceIndex : i;
            }

            accumulatedWidth += charWidth;

            if (Character.isWhitespace(ch)) {
                lastSpaceIndex = i;

                // Look ahead to next word
                int j = i + 1;
                float nextWordWidth = 0f;
                while (j < text.length() && !Character.isWhitespace(text.charAt(j))) {
                    nextWordWidth += baseFont.getWidthPoint(String.valueOf(text.charAt(j)), fontSize);
                    j++;
                }

                // Use same narrower threshold for deciding break at space
                if (accumulatedWidth + nextWordWidth > maxWidthPoints - extraMargin) {
                    return lastSpaceIndex;
                }
            }
        }
        return text.length();
    }

    private void addSPUSections(PdfWriter writer, Document document, OrderSPUDto orderSPUDto, OrderDto orderDto) throws DocumentException {
        Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100.0F);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(15);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setBorderWidth(1.0F);
        cell.setMinimumHeight(130.0F);
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDate = sdf.format(currentDate);
        Paragraph printedDatePara = new Paragraph("Afgedrukt op:    " + formattedDate);
        printedDatePara.setSpacingAfter(12f);
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
        cell3.setMinimumHeight(45.0F);
        Paragraph textParagraph3 = new Paragraph("Datum klaar:", font4);
        cell3.addElement(textParagraph3);
        table3.addCell(cell3);
        table3.setSpacingAfter(10.0F);
        document.add(table3);
    }
}
