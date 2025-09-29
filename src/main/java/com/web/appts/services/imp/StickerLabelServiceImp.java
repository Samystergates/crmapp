
package com.web.appts.services.imp;

import com.itextpdf.text.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.web.appts.DTO.StickerLabelDto;
import com.web.appts.entities.StickerLabel;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.StickerRepo;
import com.web.appts.services.StickerLabelService;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

@Service
public class StickerLabelServiceImp implements StickerLabelService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private StickerRepo stickerRepo;
    Map<String, StickerLabelDto> stickerMap = new HashMap();

    public StickerLabelServiceImp() {
    }

    private static int customHash(String input) {
        int hash = 0;
        char[] var2 = input.toCharArray();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            char c = var2[var4];
            hash = 31 * hash + c;
        }

        return hash;
    }

    public StickerLabel dtoToSLabel(StickerLabelDto sLabelDto) {
        int hashCode = sLabelDto.getProduct().hashCode();
        long barcodeNumber = Math.abs((long)hashCode);
        int customHashCode = customHash(sLabelDto.getName());
        if (String.valueOf(customHashCode).length() > 5 && String.valueOf(barcodeNumber).length() > 5) {
            sLabelDto.setBarCode(String.valueOf(customHashCode).substring(0, 5) + String.valueOf(barcodeNumber).substring(0, 5));
        } else {
            sLabelDto.setBarCode(String.valueOf(customHashCode).substring(0, 2) + String.valueOf(barcodeNumber).substring(0, 2));
        }

        StickerLabel sLabel = (StickerLabel)this.modelMapper.map(sLabelDto, StickerLabel.class);
        return sLabel;
    }

    public StickerLabelDto sLabelToDto(StickerLabel sLabel) {
        StickerLabelDto sLabelDto = (StickerLabelDto)this.modelMapper.map(sLabel, StickerLabelDto.class);
        return sLabelDto;
    }

    public StickerLabelDto createStickerLabel(StickerLabelDto stickerLabelDto) {
        List<StickerLabelDto> list = this.getAllStickerLabels();
        StickerLabelDto sLabelDtoMapVal = this.getOrderSticker(stickerLabelDto.getProduct());
        if (sLabelDtoMapVal == null) {
            StickerLabel StickerLabelSaved = (StickerLabel)this.stickerRepo.save(this.dtoToSLabel(stickerLabelDto));
            this.stickerMap.put(stickerLabelDto.getProduct(), this.sLabelToDto(StickerLabelSaved));
            return this.sLabelToDto(StickerLabelSaved);
        } else {
            stickerLabelDto.setId(sLabelDtoMapVal.getId());
            return this.updateStickerLabel(stickerLabelDto);
        }
    }

    public StickerLabelDto updateStickerLabel(StickerLabelDto stickerLabelDto) {
        List<StickerLabelDto> list = this.getAllStickerLabels();
        StickerLabel StickerLabelUpdated = (StickerLabel)this.stickerRepo.save(this.dtoToSLabel(stickerLabelDto));
        this.stickerMap.put(stickerLabelDto.getProduct(), stickerLabelDto);
        return this.sLabelToDto(StickerLabelUpdated);
    }

    public Boolean deleteStickerLabel(Long orderStickerId) {
        List<StickerLabelDto> list = this.getAllStickerLabels();
        StickerLabel stickerLabel = (StickerLabel)this.stickerRepo.findById(orderStickerId).orElseThrow(() -> {
            return new ResourceNotFoundException("orderSticker", "id", (long)orderStickerId.intValue());
        });
        this.stickerRepo.delete(stickerLabel);
        this.stickerMap.remove(stickerLabel.getProduct());
        return true;
    }

    public StickerLabelDto getOrderSticker(String prodNumber) {
        if (!this.stickerMap.isEmpty() && this.stickerMap.containsKey(prodNumber)) {
            return (StickerLabelDto)this.stickerMap.get(prodNumber);
        } else {
            StickerLabel stickerLabel = this.stickerRepo.findByProduct(prodNumber);
            return stickerLabel == null ? null : this.sLabelToDto(stickerLabel);
        }
    }

    public List<StickerLabelDto> getAllStickerLabels() {
        if (!this.stickerMap.isEmpty()) {
            return new ArrayList(this.stickerMap.values());
        } else {
            List<StickerLabel> listSLabel = this.stickerRepo.findAll();
            List<StickerLabelDto> stickerDtos = (List)listSLabel.stream().map((sticker) -> {
                StickerLabelDto stickerDto = this.sLabelToDto(sticker);
                return stickerDto;
            }).collect(Collectors.toList());
            Iterator var3 = stickerDtos.iterator();

            while(var3.hasNext()) {
                StickerLabelDto stickerDto = (StickerLabelDto)var3.next();
                this.stickerMap.put(stickerDto.getProduct(), stickerDto);
            }

            return stickerDtos;
        }
    }

    public byte[] generateStickerPdf(String key, String orderNumber) {
        List<StickerLabelDto> list = this.getAllStickerLabels();
        StickerLabelDto sLabelDto = (StickerLabelDto)this.stickerMap.get(key);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] var8;
            try {
                Rectangle pageSize = new Rectangle(600.0F, 550.0F);
                Document document = new Document(pageSize);
                PdfWriter writer = PdfWriter.getInstance(document, outputStream);
                document.open();
                this.addHeadingAndAddress(writer, document, "Montage Order", sLabelDto);
                this.addProductInfo(document, sLabelDto);
                this.addOptions1(writer, document, sLabelDto);
                this.addOptions2(writer, document, sLabelDto);
                this.addFooterandAddress(document, sLabelDto, orderNumber);
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
            ((Exception)e).printStackTrace();
            return new byte[0];
        }
    }

    private void addHeadingAndAddress(PdfWriter writer, Document document, String heading, StickerLabelDto sLabelDto) throws DocumentException, IOException {
        Font font = new Font(FontFamily.HELVETICA, 30.0F, 1);
        Font font2 = new Font(FontFamily.HELVETICA, 13.0F);
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100.0F);

        PdfPCell cell1 = new PdfPCell();
        Paragraph paragraph = new Paragraph("", font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        cell1.addElement(paragraph);
        cell1.setBorder(0);

        PdfContentByte cb = writer.getDirectContent();
        Barcode128 barcode = new Barcode128();
        barcode.setCode(sLabelDto.getProduct());
        barcode.setFont(null);
        barcode.setBaseline(0.0F);
        barcode.setBarHeight(30.0F);
        Image barcodeImage = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        barcodeImage.setAlignment(Element.ALIGN_CENTER);
        barcodeImage.scalePercent(120.0F);
        cell1.addElement(barcodeImage);

        Paragraph labelParagraph = new Paragraph(String.format("%-1s%-1s", " ", sLabelDto.getProduct()), font2);
        labelParagraph.setAlignment(Element.ALIGN_CENTER);
        cell1.addElement(labelParagraph);
        cell1.setBorder(8);
        cell1.setBorderColor(BaseColor.BLACK);
        mainTable.addCell(cell1);

        PdfPCell cell3 = new PdfPCell();

        InputStream imageStream = getClass().getClassLoader().getResourceAsStream("images/trioliet.jpg");
        if (imageStream == null) {
            throw new FileNotFoundException("Image not found: images/trioliet.jpg");
        }

        Image logoImage = Image.getInstance(ImageIO.read(imageStream), null);
        logoImage.scaleToFit(150, 150);
        logoImage.setAlignment(Element.ALIGN_CENTER);
        cell3.addElement(logoImage);
        cell3.setBorder(0);
        mainTable.addCell(cell3);

        mainTable.setSpacingAfter(10.0F);
        PdfPTable separatorTable = new PdfPTable(1);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(2);
        separatorCell.setBorderColor(BaseColor.BLACK);
        separatorTable.addCell(separatorCell);
        separatorTable.setWidthPercentage(100.0F);
        separatorTable.setSpacingAfter(10.0F);

        document.add(mainTable);
        document.add(separatorTable);
    }


    private void addProductInfo(Document document, StickerLabelDto sLabelDto) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 26.0F, 1);
        Font font2 = new Font(FontFamily.HELVETICA, 14.0F);
        Paragraph paragraph3 = new Paragraph("Produkt:", font2);
        paragraph3.setAlignment(0);
        Paragraph paragraph4 = new Paragraph("" + sLabelDto.getBandenmaat() + " - " + sLabelDto.getType() + " - " + sLabelDto.getAansluitmaat() + " - " + sLabelDto.getEt() + "", font);
        paragraph4.setAlignment(0);
        paragraph4.setSpacingAfter(10.0F);
        PdfPTable separatorTable = new PdfPTable(1);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(2);
        separatorCell.setBorderColor(BaseColor.BLACK);
        separatorTable.addCell(separatorCell);
        separatorTable.setWidthPercentage(100.0F);
        separatorTable.setSpacingAfter(10.0F);
        document.add(paragraph3);
        document.add(paragraph4);
        document.add(separatorTable);
    }

    private void addOptions1(PdfWriter writer, Document document, StickerLabelDto sLabelDto) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 26.0F, 1);
        Font font2 = new Font(FontFamily.HELVETICA, 14.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Paragraph labelParagraph = new Paragraph("Bandenmaat:", font2);
        labelParagraph.setAlignment(1);
        Paragraph dataParagraph = new Paragraph(sLabelDto.getBandenmaat(), font);
        dataParagraph.setAlignment(1);
        cell1.addElement(labelParagraph);
        cell1.addElement(dataParagraph);
        cell1.setBorder(8);
        cell1.setBorderColor(BaseColor.BLACK);
        mainTable.addCell(cell1);
        PdfPCell cell2 = new PdfPCell();
        Paragraph labelParagraph2 = new Paragraph("Type:", font2);
        labelParagraph2.setAlignment(1);
        Paragraph dataParagraph2 = new Paragraph(sLabelDto.getType(), font);
        dataParagraph2.setAlignment(1);
        cell2.addElement(labelParagraph2);
        cell2.addElement(dataParagraph2);
        cell2.setBorder(8);
        cell2.setBorderColor(BaseColor.BLACK);
        mainTable.addCell(cell2);
        PdfPCell cell3 = new PdfPCell();
        Paragraph labelParagraph3 = new Paragraph("Load Index:", font2);
        labelParagraph3.setAlignment(1);
        Paragraph dataParagraph3 = new Paragraph(sLabelDto.getLoadIndex(), font);
        dataParagraph3.setAlignment(1);
        cell3.addElement(labelParagraph3);
        cell3.addElement(dataParagraph3);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        PdfPTable separatorTable = new PdfPTable(1);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(2);
        separatorCell.setBorderColor(BaseColor.BLACK);
        separatorTable.addCell(separatorCell);
        separatorTable.setWidthPercentage(100.0F);
        separatorTable.setSpacingAfter(10.0F);
        mainTable.setSpacingAfter(10.0F);
        document.add(mainTable);
        document.add(separatorTable);
    }

    private void addOptions2(PdfWriter writer, Document document, StickerLabelDto sLabelDto) throws DocumentException {
        Font font = new Font(FontFamily.HELVETICA, 26.0F, 1);
        Font font2 = new Font(FontFamily.HELVETICA, 14.0F);
        PdfPTable mainTable = new PdfPTable(3);
        mainTable.setWidthPercentage(100.0F);
        PdfPCell cell1 = new PdfPCell();
        Paragraph labelParagraph = new Paragraph("Positie:", font2);
        labelParagraph.setAlignment(1);
        Paragraph dataParagraph = new Paragraph(sLabelDto.getPositie(), font);
        dataParagraph.setAlignment(1);
        cell1.addElement(labelParagraph);
        cell1.addElement(dataParagraph);
        cell1.setBorder(8);
        cell1.setBorderColor(BaseColor.BLACK);
        mainTable.addCell(cell1);
        PdfPCell cell2 = new PdfPCell();
        Paragraph labelParagraph2 = new Paragraph("Aansluitmaten:", font2);
        labelParagraph2.setAlignment(1);
        Paragraph dataParagraph2 = new Paragraph(sLabelDto.getAansluitmaat(), font);
        dataParagraph2.setAlignment(1);
        cell2.addElement(labelParagraph2);
        cell2.addElement(dataParagraph2);
        cell2.setBorder(8);
        cell2.setBorderColor(BaseColor.BLACK);
        mainTable.addCell(cell2);
        PdfPCell cell3 = new PdfPCell();
        Paragraph labelParagraph3 = new Paragraph("ET:", font2);
        labelParagraph3.setAlignment(1);
        Paragraph dataParagraph3 = new Paragraph(sLabelDto.getEt(), font);
        dataParagraph3.setAlignment(1);
        cell3.addElement(labelParagraph3);
        cell3.addElement(dataParagraph3);
        cell3.setBorder(0);
        mainTable.addCell(cell3);
        PdfPTable separatorTable = new PdfPTable(1);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(2);
        separatorCell.setBorderColor(BaseColor.BLACK);
        separatorTable.addCell(separatorCell);
        separatorTable.setWidthPercentage(100.0F);
        separatorTable.setSpacingAfter(10.0F);
        mainTable.setSpacingAfter(10.0F);
        document.add(mainTable);
        document.add(separatorTable);
    }

    private void addFooterandAddress(Document document, StickerLabelDto sLabelDto, String orderNumber) throws DocumentException, IOException {
        Font font1 = new Font(FontFamily.COURIER, 15.0F, Font.BOLD);

        Font font2 = new Font(FontFamily.HELVETICA, 10.0F);
        Font font3 = new Font(FontFamily.COURIER, 35.0F, Font.BOLD);
        Font font22 = new Font(FontFamily.COURIER, 30.0F, Font.BOLD);

        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100.0F);

        // Create address cell
        PdfPCell addressCell = new PdfPCell();
        addressCell.addElement(new Paragraph(orderNumber, font1));
        addressCell.addElement(new Paragraph("De Molen Banden B.V.", font2));
        addressCell.addElement(new Paragraph("Rustvenseweg 2", font2));
        addressCell.addElement(new Paragraph("5375 KW REEK", font2));
        addressCell.addElement(new Paragraph("Nederland", font2));
        addressCell.setBorder(8);
        addressCell.setBorderColor(BaseColor.BLACK);
        addressCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        mainTable.addCell(addressCell);

        // Create image cell
        PdfPCell imageCell = new PdfPCell();
        imageCell.setBorder(0);
        imageCell.setPaddingTop(20);

        try (InputStream imageStream = getClass().getClassLoader().getResourceAsStream("images/demolen.jpg")) {
            if (imageStream == null) {
                throw new FileNotFoundException("Image not found: images/demolen.jpg");
            }

            Image logoImage = Image.getInstance(ImageIO.read(imageStream), null);
            logoImage.scaleToFit(200, 200);
            logoImage.setAlignment(Element.ALIGN_CENTER);
            imageCell.addElement(logoImage);
        }

        mainTable.addCell(imageCell);
        document.add(mainTable);
    }

}
