
package com.web.appts.services.imp;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.web.appts.DTO.CustomOrderDto;
import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderTRADto;
import com.web.appts.DTO.TransportOrderLinesDto;
import com.web.appts.entities.CustomOrder;
import com.web.appts.entities.OrderTRA;
import com.web.appts.entities.TransportOrderLines;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.CustomOrderRepo;
import com.web.appts.repositories.OrderTRARepo;
import com.web.appts.services.OrderTRAService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.web.appts.utils.OrderType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderTRAServiceImp implements OrderTRAService {
	@Autowired
	OrderTRARepo orderTRARepo;
	@Autowired
	private ModelMapper modelMapper;
	@Autowired
	OrderServiceImp orderServiceImp;
	@Autowired
	CustomOrderRepo customOrderRepo;
	Map<String, OrderTRADto> traOrdersMap = new HashMap();

	public OrderTRAServiceImp() {
	}

	public OrderTRADto createOrderTRA(OrderTRADto orderTRADto) {
		OrderTRA orderTRA = this.dtoToTra(orderTRADto);

		if (orderTRA.getOrderIds() != null) {
			for (TransportOrderLines line : orderTRA.getOrderIds()) {
				line.setOrderTra(orderTRA);
			}
		}

		OrderTRA savedOrderTRA = (OrderTRA)this.orderTRARepo.save(orderTRA);
		if (!this.traOrdersMap.isEmpty()) {
			boolean idExists = this.traOrdersMap.values().stream().anyMatch((val) -> {
				return val.getId() == orderTRADto.getId();
			});
			if (!idExists) {
				this.traOrdersMap.put(savedOrderTRA.getId() + "," + savedOrderTRA.getRouteDate() + "," + savedOrderTRA.getRoute(), this.traToDto(savedOrderTRA));
			}
		}

		return this.traToDto(savedOrderTRA);
	}

	public OrderTRADto updateOrderTRA(OrderTRADto orderTRADto) {
		OrderTRA orderTRA = this.dtoToTra(orderTRADto);
		if (orderTRA.getOrderIds() != null) {
			for (TransportOrderLines line : orderTRA.getOrderIds()) {
					line.setOrderTra(orderTRA);
			}
		}
		OrderTRA savedOrderTRA = (OrderTRA)this.orderTRARepo.save(orderTRA);
		Map<String, OrderTRADto> filteredMap = (Map)this.traOrdersMap.values().stream().filter((val) -> {
			return val.getId() != orderTRADto.getId();
		}).collect(Collectors.toMap((val) -> {
			return val.getId() + "," + val.getRouteDate() + "," + val.getRoute();
		}, Function.identity()));
		this.traOrdersMap.clear();
		this.traOrdersMap.putAll(filteredMap);
		this.traOrdersMap.put(savedOrderTRA.getId() + "," + savedOrderTRA.getRouteDate() + "," + savedOrderTRA.getRoute(), this.traToDto(savedOrderTRA));
		return this.traToDto(savedOrderTRA);
	}

	public Boolean deleteOrderTRA(Long orderTRAId) {
		OrderTRA orderTRA = (OrderTRA)this.orderTRARepo.findById(orderTRAId).orElseThrow(() -> {
			return new ResourceNotFoundException("orderTRA", "id", (long)orderTRAId.intValue());
		});
		this.orderTRARepo.delete(orderTRA);
		Map<String, OrderTRADto> filteredMap = (Map)this.traOrdersMap.values().stream().filter((val) -> {
			return val.getId() != orderTRA.getId();
		}).collect(Collectors.toMap((val) -> {
			return val.getId() + "," + val.getRouteDate() + "," + val.getRoute();
		}, Function.identity()));
		this.traOrdersMap.clear();
		this.traOrdersMap.putAll(filteredMap);
		return true;
	}

	@Override
	public Boolean deleteLineFromTra(TransportOrderLines transportOrderLines){

		if (!this.traOrdersMap.isEmpty()) {
			boolean idExists = this.traOrdersMap.values().stream().anyMatch((val) -> {
				return val.getId() == transportOrderLines.getOrderTra().getId();
			});
			if (idExists) {
				Long lineIdToDelete = transportOrderLines.getId();
				long parentId = transportOrderLines.getOrderTra().getId();

				this.traOrdersMap.values().stream()
						.filter(dto -> dto.getId() == parentId)
						.findFirst()
						.ifPresent(dto -> {
							dto.getOrderIds().removeIf(lineDto -> lineDto.getId() == lineIdToDelete);
						});
			}
		}
		return true;
	}

	public OrderTRADto getOrderTRA(Long orderTRAId) {
		OrderTRA orderTRA = (OrderTRA)this.orderTRARepo.findById(orderTRAId).orElseThrow(() -> {
			return new ResourceNotFoundException("orderTRA", "id", (long)orderTRAId.intValue());
		});
		return this.traToDto(orderTRA);
	}

	public Map<String, OrderTRADto> getAllTraOrders() {
		if (!this.traOrdersMap.isEmpty()) {
			return this.traOrdersMap;
		} else {
			List<OrderTRA> allTraOrders = this.orderTRARepo.findAll();
			if (!allTraOrders.isEmpty() && allTraOrders != null) {
				Iterator var2 = allTraOrders.iterator();
				while(var2.hasNext()) {
					OrderTRA orderTRA = (OrderTRA)var2.next();
					this.traOrdersMap.put(orderTRA.getId() + "," + orderTRA.getRouteDate() + "," + orderTRA.getRoute(), this.traToDto(orderTRA));
				}
				return this.traOrdersMap;
			} else {
				return null;
			}
		}
	}

	public OrderTRA dtoToTra(OrderTRADto orderTRADto) {
		OrderTRA trailerInfo = (OrderTRA)this.modelMapper.map(orderTRADto, OrderTRA.class);
		return trailerInfo;
	}

	public OrderTRADto traToDto(OrderTRA orderTRA) {
		OrderTRADto orderTRADto = (OrderTRADto)this.modelMapper.map(orderTRA, OrderTRADto.class);
		return orderTRADto;
	}

	public Boolean updateOrderTRAColors(String orderTRAIds, Long id) {
		return this.orderServiceImp.updateTraColors(orderTRAIds, id);
	}

	public byte[] generateTRAPdf(OrderTRADto orderTRADto) {
		List<TransportOrderLinesDto> list = orderTRADto.getOrderIds();

		Map<String, OrderDto> ordersMap = this.orderServiceImp.getMap();

		List<OrderDto> mainObjects = list.stream()
				.map(ol -> ordersMap.values().stream()
						.filter(order -> order.getId() == ol.getOrderId() && ol.getOrderType().equals(OrderType.MAIN))
						.findFirst()
						.orElse(null)
				)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		List<CustomOrder> customOrderDtos = customOrderRepo.findAll();

		List<CustomOrder> custObjects = list.stream()
				.map(ol -> customOrderDtos.stream()
						.filter(order -> order.getId() == ol.getOrderId() && ol.getOrderType().equals(OrderType.CUSTOM))
						.findFirst()
						.orElse(null)
				)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());


		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			byte[] var18;
			try {
				Document document = new Document();
				PdfWriter.getInstance(document, outputStream);
				document.open();
				this.addHeadingAndAddress(document, "Dagrapport", orderTRADto);
				this.addAdditionalInformation(document, orderTRADto);
				this.addOrdersTable(document, list, mainObjects, custObjects);
				System.out.println("Closing Document");
				document.close();
				var18 = outputStream.toByteArray();
			} catch (Throwable var11) {
				try {
					outputStream.close();
				} catch (Throwable var10) {
					var11.addSuppressed(var10);
				}

				throw var11;
			}

			outputStream.close();
			return var18;
		} catch (IOException | DocumentException var12) {
			((Exception)var12).printStackTrace();
			return new byte[0];
		}
	}

	private void addHeadingAndAddress(Document document, String heading, OrderTRADto orderTRADto) throws DocumentException {
		Font font = new Font(FontFamily.HELVETICA, 18.0F, 1);
		Font font2 = new Font(FontFamily.HELVETICA, 10.0F);
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
		Paragraph paragraph22 = new Paragraph(orderTRADto.getRoute(), font2);
		paragraph2.setAlignment(1);
		paragraph22.setAlignment(1);
		cell2.addElement(paragraph2);
		cell2.addElement(paragraph22);
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

	private void addAdditionalInformation(Document document, OrderTRADto orderTRADto) throws DocumentException {
		LocalDateTime dateTime = LocalDateTime.parse(orderTRADto.getRouteDate(), DateTimeFormatter.ISO_DATE_TIME);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String formattedDate = dateTime.format(dateFormatter);
		Font font = new Font(FontFamily.HELVETICA, 10.0F);
		PdfPTable mainTable = new PdfPTable(2);
		mainTable.setWidthPercentage(100.0F);
		PdfPCell cell1 = new PdfPCell();
		Paragraph paragraphL1 = new Paragraph(String.format("%-30s%-30s", "Datum:", formattedDate), font);
		Paragraph paragraphL2 = new Paragraph(String.format("\n%-29s%-29s", "Chauffeur:", orderTRADto.getChauffeur()), font);
		String result = "";
		if (orderTRADto.getTruck() != null) {
			String[] parts = orderTRADto.getTruck().split(",");
			result = parts[0].trim();
		}

		Paragraph paragraphL3 = new Paragraph(String.format("\n%-29s%-29s", "Kenteken:", result), font);
		Paragraph paragraphL4 = new Paragraph(String.format("\n%-30s%-30s", "Oplegger:", orderTRADto.getTrailer()), font);
		Paragraph paragraphL5 = new Paragraph(String.format("\n%-25s%-25s", "Aavang weektijd", "____________________"), font);
		Paragraph paragraphL6 = new Paragraph(String.format("\n%-27s%-27s", "Einde weektijd:", "____________________"), font);
		paragraphL1.setAlignment(0);
		paragraphL2.setAlignment(0);
		paragraphL3.setAlignment(0);
		paragraphL4.setAlignment(0);
		paragraphL5.setAlignment(0);
		paragraphL6.setAlignment(0);
		cell1.addElement(paragraphL1);
		cell1.addElement(paragraphL2);
		cell1.addElement(paragraphL3);
		cell1.addElement(paragraphL4);
		cell1.addElement(paragraphL5);
		cell1.addElement(paragraphL6);
		cell1.setBorder(0);
		mainTable.addCell(cell1);
		PdfPCell cell2 = new PdfPCell();
		Paragraph paragraph2 = new Paragraph(String.format("%-10s%-10s", "Eindstand KM:", "________________________"), font);
		Paragraph paragraph3 = new Paragraph(String.format("\n%-10s%-10s", "Beginstand KM:", "________________________"), font);
		Paragraph paragraph4 = new Paragraph(String.format("\n%-10s%-10s", "Aantal KM:", "________________________"), font);
		Paragraph paragraph5 = new Paragraph(String.format("\n%-10s%-10s", "Kosten:", "________________________"), font);
		Paragraph paragraph6 = new Paragraph(String.format("\n%-10s%-10s", "Aanvang pauze:", "________________________"), font);
		Paragraph paragraph7 = new Paragraph(String.format("\n%-10s%-10s", "Einde pauze:", "________________________"), font);
		paragraph2.setAlignment(2);
		paragraph3.setAlignment(2);
		paragraph4.setAlignment(2);
		paragraph5.setAlignment(2);
		paragraph6.setAlignment(2);
		paragraph7.setAlignment(2);
		cell2.addElement(paragraph2);
		cell2.addElement(paragraph3);
		cell2.addElement(paragraph4);
		cell2.addElement(paragraph5);
		cell2.addElement(paragraph6);
		cell2.addElement(paragraph7);
		cell2.setBorder(0);
		mainTable.addCell(cell2);
		mainTable.setSpacingAfter(40.0F);
		document.add(mainTable);
	}

	private void addOrdersTable(Document document, List<TransportOrderLinesDto> lines ,List<OrderDto> objects, List<CustomOrder> objectsB) throws DocumentException {
		PdfPTable table = new PdfPTable(6);
		table.setWidthPercentage(100.0F);
		float[] columnWidths = new float[]{10.0F, 35.0F, 10.0F, 19.0F, 7.0F, 19.0F};
		table.setWidths(columnWidths);
		table.setSpacingBefore(3.0F);
		table.setSpacingAfter(3.0F);
		Font font2 = new Font(FontFamily.HELVETICA, 10.0F);
		Font font3 = new Font(FontFamily.HELVETICA, 10.0F, 1);
		PdfPCell headerCell = this.createHeaderCell("Order", font3);
		PdfPCell headerCell2 = this.createHeaderCell("Naam", font3);
		PdfPCell headerCell3 = this.createHeaderCell("Postcode", font3);
		PdfPCell headerCell4 = this.createHeaderCell("Plaat", font3);
		PdfPCell headerCell5 = this.createHeaderCell("Land", font3);
		PdfPCell headerCell6 = this.createHeaderCell("Opmerking", font3);
		table.addCell(headerCell);
		table.addCell(headerCell2);
		table.addCell(headerCell3);
		table.addCell(headerCell4);
		table.addCell(headerCell5);
		table.addCell(headerCell6);

//		for(int i = 0; i < 20; ++i) {
//			PdfPCell cell;
//			if (i < objects.size()) {
//				PdfPCell cell1 = this.createCell(((OrderDto)objects.get(i)).getOrderNumber(), font2);
//				cell = this.createCell(((OrderDto)objects.get(i)).getCustomerName(), font2);
//				PdfPCell cell3 = this.createCell(((OrderDto)objects.get(i)).getPostCode(), font2);
//				PdfPCell cell4 = this.createCell(((OrderDto)objects.get(i)).getCity(), font2);
//				PdfPCell cell5 = this.createCell(((OrderDto)objects.get(i)).getCountry(), font2);
//				PdfPCell cell6 = this.createCell("", font2);
//				table.addCell(cell1);
//				table.addCell(cell);
//				table.addCell(cell3);
//				table.addCell(cell4);
//				table.addCell(cell5);
//				table.addCell(cell6);
//				cell1.setFixedHeight(20.0F);
//				cell.setFixedHeight(20.0F);
//				cell3.setFixedHeight(20.0F);
//				cell4.setFixedHeight(20.0F);
//				cell5.setFixedHeight(20.0F);
//				cell6.setFixedHeight(20.0F);
//			} else if (objects.size() < 10) {
//				for(int j = 0; j < 6; ++j) {
//					cell = this.createCell("", font2);
//					cell.setFixedHeight(20.0F);
//					table.addCell(cell);
//				}
//			}
//		}

		lines.sort(Comparator.comparingInt(TransportOrderLinesDto::getOrderRowNumber));
		for (int i = 0; i < 20; ++i) {
			PdfPCell cell;
			if (i < lines.size()) {
				TransportOrderLinesDto line = lines.get(i);
				OrderType orderType = line.getOrderType();

				String orderNumber = "";
				String customerName = "";
				String postCode = "";
				String city = "";
				String country = "";
				String opmerking = "";

				if (orderType == OrderType.MAIN) {
					OrderDto order = objects.stream()
							.filter(o -> o.getId() == line.getOrderId())
							.findFirst()
							.orElse(null);
					if (order != null) {
						orderNumber = order.getOrderNumber();
						customerName = order.getCustomerName();
						postCode = order.getPostCode();
						city = order.getCity();
						country = order.getCountry();
					}
				} else if (orderType == OrderType.CUSTOM) {
					CustomOrder custom = objectsB.stream()
							.filter(c -> c.getId() == line.getOrderId())
							.findFirst()
							.orElse(null);
					if (custom != null) {
						orderNumber = custom.getOrderNumber();
						customerName = custom.getCustomerName();
						postCode = custom.getPostCode();
						city = custom.getCity();
						country = custom.getCountry();
						opmerking = custom.getOpmerking();
					}
				}

				PdfPCell cell1 = this.createCell(orderNumber, font2);
				cell = this.createCell(customerName, font2);
				PdfPCell cell3 = this.createCell(postCode, font2);
				PdfPCell cell4 = this.createCell(city, font2);
				PdfPCell cell5 = this.createCell(country, font2);
				PdfPCell cell6 = this.createCell(opmerking, font2);

				table.addCell(cell1);
				table.addCell(cell);
				table.addCell(cell3);
				table.addCell(cell4);
				table.addCell(cell5);
				table.addCell(cell6);

				cell1.setFixedHeight(20.0F);
				cell.setFixedHeight(20.0F);
				cell3.setFixedHeight(20.0F);
				cell4.setFixedHeight(20.0F);
				cell5.setFixedHeight(20.0F);
				cell6.setFixedHeight(20.0F);
			} else {
				for (int j = 0; j < 6; ++j) {
					cell = this.createCell("", font2);
					cell.setFixedHeight(20.0F);
					table.addCell(cell);
				}
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
}
