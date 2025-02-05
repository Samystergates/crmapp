package com.web.appts;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ApptsApplication {
	@Autowired
	private ApplicationContext context;
	public static void main(String[] args) {
		SpringApplication.run(ApptsApplication.class, args);

		System.out.println("response ok");
//		int i = 0;
//		String excelFilePath = "C:\\Users\\Samu degozaru\\Desktop\\Book1.xlsx";
//		String jdbcUrl = "jdbc:mysql://localhost:3306/dataprd";
//		String username = "root";
//		String password = "root";
//
//		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
//				FileInputStream excelFile = new FileInputStream(excelFilePath);
//				Workbook workbook = new XSSFWorkbook(excelFile)) {
//
//			// Assuming the data is in the first sheet (index 0)
//			Sheet sheet = workbook.getSheetAt(0);
//
//			// Loop through rows (starting from row 1 to skip headers)
//			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
//				Row row = sheet.getRow(rowIndex);
//				if (row != null) {
//					// Customize this part to match your Excel columns and database table columns
//					int id = ++i;
//					String merk = getCellValue(row.getCell(0));
//					String model = getCellValue(row.getCell(1));
//					String type = getCellValue(row.getCell(2));
//					String bandenmaat = getCellValue(row.getCell(3));
//					String velgmaat = getCellValue(row.getCell(4));
//					String velgtype = getCellValue(row.getCell(5));
//					String velgnummer = getCellValue(row.getCell(6));
//					String velgleverancier = getCellValue(row.getCell(7));
//					String department = getCellValue(row.getCell(8));
//					String naafgat = getCellValue(row.getCell(9));
//					String steek = getCellValue(row.getCell(10));
//					String aantalBoutgat = getCellValue(row.getCell(11));
//					String verdlingBoutgaten = getCellValue(row.getCell(12));
//					String diameter = getCellValue(row.getCell(13));
//					String typeBoutgat = getCellValue(row.getCell(14));
//					String maatVerzinking = getCellValue(row.getCell(15));
//					String et = getCellValue(row.getCell(16));
//					String afstandVV = getCellValue(row.getCell(17));
//					String afstandVA = getCellValue(row.getCell(18));
//					String uitvoerechtingFlens = getCellValue(row.getCell(19));
//					String dikte = getCellValue(row.getCell(20));
//					String koelgaten = getCellValue(row.getCell(21));
//					String opmerking = getCellValue(row.getCell(22));
//					String insertQuery = "INSERT INTO dataprd.wheel_machine_size (id, merk, model, type, bandenmaat, velgmaat,velguitvoering,velgnummer,velgleverancier,Afdeling,naafgat,steekcirkel,aantalboutgaten,verdlingboutgaten,diameterboutgat,uitvoeringboutgat,maatverzinking,offset,afstandvoorzijde,afstandachterzijde,flensuitvoering,dikteschijf,koelgaten,opmerking) VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?)";
//
//					try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
//						 preparedStatement.setInt(1, id);
//						    preparedStatement.setString(2, merk);
//						    preparedStatement.setString(3, model);
//						    preparedStatement.setString(4, type);
//						    preparedStatement.setString(5, bandenmaat);
//						    preparedStatement.setString(6, velgmaat);
//						    preparedStatement.setString(7, velgtype);
//						    preparedStatement.setString(8, velgnummer);
//						    preparedStatement.setString(9, velgleverancier);
//						    preparedStatement.setString(10, department);
//						    preparedStatement.setString(11, naafgat);
//						    preparedStatement.setString(12, steek);
//						    preparedStatement.setString(13, aantalBoutgat);
//						    preparedStatement.setString(14, verdlingBoutgaten);
//						    preparedStatement.setString(15, diameter);
//						    preparedStatement.setString(16, typeBoutgat);
//						    preparedStatement.setString(17, maatVerzinking);
//						    preparedStatement.setString(18, et);
//						    preparedStatement.setString(19, afstandVV);
//						    preparedStatement.setString(20, afstandVA);
//						    preparedStatement.setString(21, uitvoerechtingFlens);
//						    preparedStatement.setString(22, dikte);
//						    preparedStatement.setString(23, koelgaten);
//						    preparedStatement.setString(24, opmerking);
//						
//						
//						preparedStatement.executeUpdate();
//					}
//				}
//			}
//
//			System.out.println("Data inserted successfully!");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static String getCellValue(Cell cell) {
//		if (cell == null) {
//			return "";
//		}
//		switch (cell.getCellType()) {
//		case STRING:
//			return cell.getStringCellValue();
//		case NUMERIC:
//			return String.valueOf(cell.getNumericCellValue());
//		default:
//			return "";
//		}
		
//		int i = 0;
//		String excelFilePath = "C:\\Users\\Samu degozaru\\Desktop\\orders.xlsx";
//		String jdbcUrl = "jdbc:mysql://localhost:3306/demotesttable";
//		String username = "root";
//		String password = "root";
//
//		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
//				FileInputStream excelFile = new FileInputStream(excelFilePath);
//				Workbook workbook = new XSSFWorkbook(excelFile)) {
//
//			// Assuming the data is in the first sheet (index 0)
//			Sheet sheet = workbook.getSheetAt(0);
//
//			// Loop through rows (starting from row 1 to skip headers)
//			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
//				Row row = sheet.getRow(rowIndex);
//				if (row != null) {
//					// Customize this part to match your Excel columns and database table columns
//					int id = ++i;
//					String Verkooporder = getCellValue(row.getCell(0));
//					String Ordersoort = getCellValue(row.getCell(1));
//					String Backorder = getCellValue(row.getCell(2));
//					String SME = getCellValue(row.getCell(3));
//					String SPE = getCellValue(row.getCell(4));
//					String MON_LB = getCellValue(row.getCell(5));
//					String MON_TR = getCellValue(row.getCell(6));
//					String MWE = getCellValue(row.getCell(7));
//					String SER = getCellValue(row.getCell(8));
//					String TRA = getCellValue(row.getCell(9));
//					String EXP = getCellValue(row.getCell(10));
//					String EXC = getCellValue(row.getCell(11));
//					String Gebruiker_I = getCellValue(row.getCell(12));
//					String Organisatie = getCellValue(row.getCell(13));
//					String Naam = getCellValue(row.getCell(14));
//					String Postcode = getCellValue(row.getCell(15));
//					String Plaats = getCellValue(row.getCell(16));
//					String Land = getCellValue(row.getCell(17));
//					String Leverdatum = getCellValue(row.getCell(18));
//					if (Leverdatum != null && !Leverdatum.isEmpty()) {
//					    // Check if the cell contains a date
//					    if (DateUtil.isCellDateFormatted(row.getCell(18))) {
//					        // Convert the serial number to a Date object
//					        Date javaDate = DateUtil.getJavaDate(Double.parseDouble(Leverdatum));
//
//					        // Format the Date object to the desired string format
//					        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
//					        Leverdatum = dateFormat.format(javaDate);
//					    }
//					} else {
//					    // Handle the case when the cell is empty or null
//					    Leverdatum = ""; // or set it to some default value
//					}
//					String Referentie = getCellValue(row.getCell(19));
//					String Datum_order = getCellValue(row.getCell(20));
//					if (Datum_order != null && !Datum_order.isEmpty()) {
//					    // Check if the cell contains a date
//					    if (DateUtil.isCellDateFormatted(row.getCell(20))) {
//					        // Convert the serial number to a Date object
//					        Date javaDate = DateUtil.getJavaDate(Double.parseDouble(Datum_order));
//
//					        // Format the Date object to the desired string format
//					        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
//					        Datum_order = dateFormat.format(javaDate);
//					    }
//					} else {
//					    // Handle the case when the cell is empty or null
//						Datum_order = ""; // or set it to some default value
//					}
//					String Datum_laatste_wijziging = getCellValue(row.getCell(21));
//					if (Datum_laatste_wijziging != null && !Datum_laatste_wijziging.isEmpty()) {
//					    // Check if the cell contains a date
//					    if (DateUtil.isCellDateFormatted(row.getCell(21))) {
//					        // Convert the serial number to a Date object
//					        Date javaDate = DateUtil.getJavaDate(Double.parseDouble(Datum_laatste_wijziging));
//
//					        // Format the Date object to the desired string format
//					        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
//					        Datum_laatste_wijziging = dateFormat.format(javaDate);
//					    }
//					} else {
//					    // Handle the case when the cell is empty or null
//						Datum_laatste_wijziging = ""; // or set it to some default value
//					}
//					String Gebruiker_L = getCellValue(row.getCell(22));
//					String Regel = getCellValue(row.getCell(23));
//					String Aantal_besteld = getCellValue(row.getCell(24));
//					String Aantal_geleverd = getCellValue(row.getCell(25));
//					String Product = getCellValue(row.getCell(26));
//					String Omschrijving = getCellValue(row.getCell(27));
//					String regelvolgorde = getCellValue(row.getCell(28));
//					String cdprodgrp = getCellValue(row.getCell(29));
//					String insertQuery = "INSERT INTO demotesttable.demotable2 (Verkooporder, Ordersoort, Backorder, SME, SPE, MON_LB, MON_TR, MWE, SER, TRA, EXP, EXC, Gebruiker_I, Organisatie, Naam, Postcode, Plaats, Land, Leverdatum, Referentie, Datum_order, Datum_laatste_wijziging, Gebruiker_L, Regel, Aantal_besteld, Aantal_geleverd, Product, Omschrijving, regelvolgorde, cdprodgrp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//					try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
//						 	//preparedStatement.setInt(1, id);
//						preparedStatement.setString(1, Verkooporder);
//						preparedStatement.setString(2, Ordersoort);
//						preparedStatement.setString(3, Backorder);
//						preparedStatement.setString(4, SME);
//						preparedStatement.setString(5, SPE);
//						preparedStatement.setString(6, MON_LB);
//						preparedStatement.setString(7, MON_TR);
//						preparedStatement.setString(8, MWE);
//						preparedStatement.setString(9, SER);
//						preparedStatement.setString(10, TRA);
//						preparedStatement.setString(11, EXP);
//						preparedStatement.setString(12, EXC);
//						preparedStatement.setString(13, Gebruiker_I);
//						preparedStatement.setString(14, Organisatie);
//						preparedStatement.setString(15, Naam);
//						preparedStatement.setString(16, Postcode);
//						preparedStatement.setString(17, Plaats);
//						preparedStatement.setString(18, Land);
//						preparedStatement.setString(19, Leverdatum);
//						preparedStatement.setString(20, Referentie);
//						preparedStatement.setString(21, Datum_order);
//						preparedStatement.setString(22, Datum_laatste_wijziging);
//						preparedStatement.setString(23, Gebruiker_L);
//						preparedStatement.setString(24, Regel);
//						preparedStatement.setString(25, Aantal_besteld);
//						preparedStatement.setString(26, Aantal_geleverd);
//						preparedStatement.setString(27, Product);
//						preparedStatement.setString(28, Omschrijving);
//						preparedStatement.setString(29, regelvolgorde);
//						preparedStatement.setString(30, cdprodgrp);
//
//						preparedStatement.executeUpdate();
//					}
//				}
//			}
//
//			System.out.println("Data inserted successfully!");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static String getCellValue(Cell cell) {
//		if (cell == null) {
//			return "";
//		}
//		switch (cell.getCellType()) {
//		case STRING:
//			return cell.getStringCellValue();
//		case NUMERIC:
//			return String.valueOf(cell.getNumericCellValue());
//		default:
//			return "";
//		}
	}

	private boolean isAppStarted = false;

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		isAppStarted = true;
		System.out.println("Application started successfully. Refresh will occur in 30 minutes.");
	}


	@Scheduled(fixedRate = 30 * 60 * 1000)
	//@Scheduled(fixedDelay = 180000) // 3 minutes in milliseconds
	public void restartApplication() {
		if (isAppStarted) {
			int exitCode = SpringApplication.exit(context, () -> 0);
			System.exit(exitCode);
		}
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

}
