
package com.web.appts.services.imp;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.web.appts.DTO.OrderDto;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
		List<OrderDto> orderList = (List)ordersMap.values().stream().filter((orderDto) -> {
			return key.equals(orderDto.getOrderNumber()) && ("Y".equals(orderDto.getMonLb()) || "Y".equals(orderDto.getMonTr()));
		}).collect(Collectors.toList());

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
				this.addSections(document);
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
			((Exception)e).printStackTrace();
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
		barcode.setCode(((OrderDto)list.get(0)).getOrderNumber());
		barcode.setFont((BaseFont)null);
		barcode.setBaseline(0.0F);
		barcode.setBarHeight(15.0F);
		PdfContentByte cb = writer.getDirectContent();
		Image image = barcode.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
		image.setAlignment(0);
		image.scalePercent(120.0F);
		cell1.addElement(image);
		Paragraph labelParagraph = new Paragraph(String.format("%-9s%-9s", " ", ((OrderDto)list.get(0)).getOrderNumber()), font2);
		labelParagraph.setAlignment(0);
		cell1.addElement(labelParagraph);
		cell1.setBorder(0);
		mainTable.addCell(cell1);
		PdfPCell cell2 = new PdfPCell();
		Paragraph paragraphL1 = new Paragraph(String.format("%-15s%-15s", heading + ":", "De Molen Beheer BV"), font4);
		Paragraph paragraphL2 = new Paragraph(String.format("%-23s%-23s", "", "Rustvenseweg 2"), font4);
		Paragraph paragraphL3 = new Paragraph(String.format("%-23s%-23s", "", "5375 KW REEK"), font4);
		paragraphL1.setAlignment(0);
		paragraphL2.setAlignment(0);
		paragraphL3.setAlignment(0);
		cell2.addElement(paragraphL1);
		cell2.addElement(paragraphL2);
		cell2.addElement(paragraphL3);
		cell2.setBorder(0);
		mainTable.addCell(cell2);
		PdfPCell cell3 = new PdfPCell();
		Paragraph paragraphR1 = new Paragraph(String.format("%-10s%-10s", "Oplegger: ", ((OrderDto)list.get(0)).getOrderNumber()), font4);
		Paragraph paragraphR2 = new Paragraph(String.format("%-10s%-10s", "Behandelaar: ", "_______"), font4);
		Paragraph paragraphR3 = new Paragraph(String.format("%-10s%-10s", "Leverdatum: ", ((OrderDto)list.get(0)).getDeliveryDate()), font4);
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
		table.setWidthPercentage(100.0F);
		float[] columnWidths = new float[]{11.0F, 11.0F, 18.0F, 60.0F};
		table.setWidths(columnWidths);
		table.setSpacingBefore(3.0F);
		table.setSpacingAfter(150.0F);
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

		for(int i = 0; i < orderList.size(); ++i) {
			if (i < orderList.size()) {
				PdfPCell cell1 = this.createCell(((OrderDto)orderList.get(i)).getRegel(), font2);
				PdfPCell cell2 = this.createCell("", font2);
				PdfPCell cell3 = this.createCell(((OrderDto)orderList.get(i)).getProduct(), font2);
				PdfPCell cell4 = this.createCell(((OrderDto)orderList.get(i)).getOmsumin(), font2);
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

		document.add(table);
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

	private void addOptions(PdfWriter writer, Document document, List<OrderDto> orderList) throws DocumentException {
		Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
		Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
		PdfContentByte cb = writer.getDirectContent();
		float cb1Y1 = 412.0F;
		float cb2Y2 = 352.0F;
		float cb3Y3 = 442.0F;
		float cb4Y4 = 412.0F;
		float cb5Y5 = 382.0F;
		float cb6Y6 = 352.0F;

		if (orderList.size() == 1) {
			cb1Y1 -= 11.5F;
			cb2Y2 -= 11.5F;
			cb3Y3 -= 11.5F;
			cb4Y4 -= 11.5F;
			cb5Y5 -= 11.5F;
			cb6Y6 -= 11.5F;
		}

		if (orderList.size() == 2) {
			cb1Y1 -= 15.5F;
			cb2Y2 -= 15.5F;
			cb3Y3 -= 15.5F;
			cb4Y4 -= 15.5F;
			cb5Y5 -= 15.5F;
			cb6Y6 -= 15.5F;
		}

		if (orderList.size() == 3) {
			cb1Y1 -= 30.0F;
			cb2Y2 -= 30.0F;
			cb3Y3 -= 30.0F;
			cb4Y4 -= 30.0F;
			cb5Y5 -= 30.0F;
			cb6Y6 -= 30.0F;
		}

		if (orderList.size() == 4) {
			cb1Y1 -= 43.5F;
			cb2Y2 -= 43.5F;
			cb3Y3 -= 43.5F;
			cb4Y4 -= 43.5F;
			cb5Y5 -= 43.5F;
			cb6Y6 -= 43.5F;
		}

		if (orderList.size() == 5) {
			cb1Y1 -= 60.0F;
			cb2Y2 -= 60.0F;
			cb3Y3 -= 60.0F;
			cb4Y4 -= 60.0F;
			cb5Y5 -= 60.0F;
			cb6Y6 -= 60.0F;
		}

		if (orderList.size() >= 6) {
			cb1Y1 -= 75.0F;
			cb2Y2 -= 75.0F;
			cb3Y3 -= 75.0F;
			cb4Y4 -= 75.0F;
			cb5Y5 -= 75.0F;
			cb6Y6 -= 75.0F;
		}

		cb.rectangle(195.0F, cb1Y1, 10.0F, 10.0F);
		cb.stroke();
		PdfContentByte cb2 = writer.getDirectContent();
		cb2.rectangle(195.0F, cb2Y2, 10.0F, 10.0F);
		cb2.stroke();
		PdfContentByte cb3 = writer.getDirectContent();
		cb3.rectangle(128.0F, cb3Y3, 10.0F, 10.0F);
		cb3.stroke();
		PdfContentByte cb4 = writer.getDirectContent();
		cb4.rectangle(128.0F, cb4Y4, 10.0F, 10.0F);
		cb4.stroke();
		PdfContentByte cb5 = writer.getDirectContent();
		cb5.rectangle(128.0F, cb5Y5, 10.0F, 10.0F);
		cb5.stroke();
		PdfContentByte cb6 = writer.getDirectContent();
		cb6.rectangle(128.0F, cb6Y6, 10.0F, 10.0F);
		cb6.stroke();
		Paragraph labelParagraph0 = new Paragraph("Product eindcontrole:", font3);
		Paragraph labelParagraph1 = new Paragraph(String.format("%-30s%-30s", "\nBand(en) schoon: ", "JA"), font4);
		Paragraph labelParagraph2 = new Paragraph("\n                                         NEE               Schoon gemaakt.", font4);
		Paragraph labelParagraph3 = new Paragraph(String.format("%-30s%-30s", "\nBeschadigingen: ", " NEE"), font4);
		Paragraph labelParagraph4 = new Paragraph("\n                                          JA                  Bijgewerkt.", font4);
		labelParagraph4.setSpacingAfter(20.0F);
		document.add(labelParagraph0);
		document.add(labelParagraph1);
		document.add(labelParagraph2);
		document.add(labelParagraph3);
		document.add(labelParagraph4);
	}

	private void addSections(Document document) throws DocumentException {
		Font font4 = new Font(FontFamily.HELVETICA, 10.0F);
		PdfPTable table = new PdfPTable(1);
		table.setWidthPercentage(100.0F);
		PdfPCell cell = new PdfPCell();
		cell.setBorder(15);
		cell.setBorderColor(BaseColor.BLACK);
		cell.setBorderWidth(1.0F);
		cell.setMinimumHeight(60.0F);
		Paragraph textParagraph = new Paragraph("Opmerking:", font4);
		cell.addElement(textParagraph);
		table.addCell(cell);
		table.setSpacingAfter(10.0F);
		PdfPTable table2 = new PdfPTable(1);
		table2.setWidthPercentage(100.0F);
		PdfPCell cell2 = new PdfPCell();
		cell2.setBorder(15);
		cell2.setBorderColor(BaseColor.BLACK);
		cell2.setBorderWidth(1.0F);
		cell2.setMinimumHeight(60.0F);
		Paragraph textParagraph2 = new Paragraph("Paraaf en naam monteur:", font4);
		cell2.addElement(textParagraph2);
		table2.addCell(cell2);
		document.add(table);
		document.add(table2);
	}
}
