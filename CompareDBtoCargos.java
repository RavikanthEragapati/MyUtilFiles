import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import us.mi.state.dhs.fw.batch.Controller.TIERSBatchController;
import us.mi.state.dhs.fw.business.exceptions.TIERSBatchException;

/**
 * This Class was written to identify discrepancies in Cargo files generated for
 * tables or any modification done to auto generated CCD's
 * 
 * @author eragapatir
 *
 */
public class CompareDBtoCargos {

	public TIERSBatchController tbc;

	// Table_Name - Stores table Name as String
	// Cargo_Fields - Stores all field Names for given cargo in a list
	// Table_Column - Stores all column Names from DB for a given Table in a
	// list
	// GenDAO_Column - Stores all the Cargo to column mapping values in a list
	// Extends - all the extend classes for a given cargo
	// Table_Type_As_Per_Cargo - Type of table by looking into cargo file
	// Table_Type_As_Per_DB - Type of table by looking into DB_TABLES table in
	// DB
	// Additional_Mapping_fields_GenDAO - Fields that are not in GenDAO that are
	// not in DB
	// Cargo_Additional_Fields - Fields that are not in cargo but not DB
	// Table_Additional_column - columns that are in DB but not in Cargo
	// Cargo_Implements - list of all the implemented interfaces by cargo
	//

	Map<String, Object> aMap;
	// PrintWriter writer;
	Connection con = null;
	// int counter = 0;
	List<String> cargoPkgs = null;
	List<String> GenDAOPkgs = null;
	// String pkg = null;
	String[][] COLUMN_MAP = null;

	List<String> errorTabList = new ArrayList<String>();
	List<List<String>> tab1List = new ArrayList<List<String>>();
	List<String> tab1Content = null;

	XSSFWorkbook workbook = new XSSFWorkbook();
	XSSFWorkbook workbookErrors = new XSSFWorkbook();
	XSSFSheet sheet = workbook.createSheet("All_Tables");
	XSSFSheet errorTab = workbookErrors.createSheet("Error");

	List<String> allGetterMethodsBasedOfFieldsInClass = null;
	List<String> allSetterMethodsBasedOfFieldsInClass = null;

	/**
	 * Default constructor
	 */
	public CompareDBtoCargos() {
		try {
			tbc = new TIERSBatchController();
			tbc.setJobId("IN-RAVIT-EST");
			con = tbc.getConnection();
		} catch (TIERSBatchException e) {
			e.printStackTrace();
			System.out.println("Exception in Constructor");
		}

	}

	/**
	 * Comparison of columns in DB tables to its respective Cargo and GenDAO
	 * files starts here
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CompareDBtoCargos comp = new CompareDBtoCargos();
		comp.process();
		System.out.println("The END");
	}

	/**
	 * Comparison process starts here by reading an input file.
	 */
	private void process() {
		cargoPkgs = new ArrayList<String>();
		GenDAOPkgs = new ArrayList<String>();
		populatePkgsForCargos(cargoPkgs);
		populatePkgsForGenDAOs(GenDAOPkgs);

		aMap = new HashMap<String, Object>();
		List<String> retainList = null;

		try (BufferedReader br = new BufferedReader(
				new FileReader("C:\\Workspace\\d4_workspace\\WF\\ejbModule\\input.txt"))) {
			// writer = new PrintWriter("output.txt", "UTF-8");
			String line;
			addHeaderToExcel();
			while ((line = br.readLine()) != null) {
				String cargoQualifiedName = getQualifiedName(line, 'C');
				String genDAOQualifiedName = getQualifiedName(line, 'G');
				if (cargoQualifiedName != null) {
					Class<?> cargo = getClassInstanceForATable(cargoQualifiedName);
					Class<?> genDOA = getClassInstanceForATable(genDAOQualifiedName);
					Object obj = genDOA.newInstance();
					Field genDAOMapFieldObj = genDOA.getField("COLUMN_MAP");
					genDAOMapFieldObj.setAccessible(true);
					String[][] genDAOMapFieldValue = null;
					genDAOMapFieldValue = (String[][]) genDAOMapFieldObj.get(obj);
					aMap.put("Table_Name", line);
					List<String> listOfFieldsFromCargo = listAllFieldNames.apply(cargo);
					generateAllPosiableMethodsInCargoByFieldNames(listOfFieldsFromCargo);
					getFieldsAndTabeTypeFromCargoExtendedClass(listOfFieldsFromCargo, cargo);
					getInterfacesImplementedByCargo(cargo);
					notSetGetMethods(cargo);
					aMap.put("Cargo_Fields", listOfFieldsFromCargo);
					List<String> listOfColumnsFromDB = getTableTypeAndColumnNamesFromDB(line);
					List<String> listOfColumnsFromDBCopy = new ArrayList<String>(listOfColumnsFromDB);
					aMap.put("Table_Column", listOfColumnsFromDB);
					List<String> listOfColumnsFromGenDAO = getColumnNamesFromGenDAO(genDAOMapFieldValue);
					List<String> listOfColumnsFromGenDAOCopy = new ArrayList<String>(listOfColumnsFromGenDAO);
					aMap.put("GenDAO_Column", listOfColumnsFromGenDAO);
					Collections.sort(listOfFieldsFromCargo);
					Collections.sort(listOfColumnsFromDB);
					Collections.sort(listOfColumnsFromGenDAO);
					listOfColumnsFromGenDAO.removeAll(listOfColumnsFromDBCopy);
					listOfColumnsFromDBCopy.removeAll(listOfColumnsFromGenDAOCopy);
					aMap.put("Additional_Mapping_fields_GenDAO", listOfColumnsFromGenDAO);
					aMap.put("Additional_fields_in_DB_based_of_GenDAO", listOfColumnsFromDBCopy);
					retainList = new ArrayList<String>(listOfColumnsFromDB);
					retainList.retainAll(listOfFieldsFromCargo);
					listOfFieldsFromCargo.removeAll(retainList);
					listOfColumnsFromDB.removeAll(retainList);
					aMap.put("Cargo_Additional_Fields", listOfFieldsFromCargo);
					aMap.put("Table_Additional_column", listOfColumnsFromDB);
					propareForXls(aMap);
					// writeToFile(aMap);
				} else {
					String[] checkTemp = null;
					checkTemp = line.split("_");
					if (checkTemp[checkTemp.length - 1].equalsIgnoreCase("A")
							|| checkTemp[checkTemp.length - 1].equalsIgnoreCase("B")) {
						// Underscore tables dont have CCD's so its not error
					} else {
						// No CCD found
						errorTabList.add(line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} finally {
			try {
				con.close();
				// writer.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		writeToExcelObject(tab1List, errorTabList);
	}

	/**
	 * This method take in all the fields in a cargo as list and identifies all
	 * possiable Setters and getters for them
	 * 
	 * @param listOfFieldsFromCargo
	 *            - list of all the fields
	 */
	private void generateAllPosiableMethodsInCargoByFieldNames(List<String> listOfFieldsFromCargo) {
		allGetterMethodsBasedOfFieldsInClass = new ArrayList<String>();
		allSetterMethodsBasedOfFieldsInClass = new ArrayList<String>();
		listOfFieldsFromCargo.forEach((s) -> {

			allGetterMethodsBasedOfFieldsInClass.add("get" + s);
			allGetterMethodsBasedOfFieldsInClass.add("is" + s);
			allSetterMethodsBasedOfFieldsInClass.add("set" + s);
		});
		// System.out.println(allGetterMethodsBasedOfFieldsInClass);

	}

	/**
	 * This method takes in a class and identifies all the interfaces it
	 * implemented and set them into a map
	 * 
	 * @param cargo
	 *            - class
	 */
	private void getInterfacesImplementedByCargo(Class<?> cargo) {
		List<String> listOfImplementedInterfacesFromCargo = new ArrayList<String>();
		Class<?>[] temp = cargo.getInterfaces();
		for (Class<?> t : temp) {
			listOfImplementedInterfacesFromCargo.add(t.getSimpleName().toString());
		}
		aMap.put("Cargo_Implements", listOfImplementedInterfacesFromCargo);
	}

	/**
	 * This method takes in a 2 dimensional array with values of DB column to
	 * Cargo Field mapping and returns a list of all the column names.
	 * 
	 * @param columnToFieldMap
	 *            - parm takes COLUMN_MAP field from GenDAO.
	 * @return - A list of Column Names from GenDAO COLUMN_MAP by removing all
	 *         the "_" from it.
	 */
	private List<String> getColumnNamesFromGenDAO(String[][] columnToFieldMap) {
		List<String> list = new ArrayList<String>();
		StringBuilder sb;
		String temp[];
		for (int i = 0; i < columnToFieldMap.length; i++) {
			sb = new StringBuilder();
			temp = columnToFieldMap[i][0].split("_");
			for (int j = 0; j < temp.length; j++) {
				if (j == 0) {
					sb.append(temp[j].toLowerCase());
				} else {
					sb.append(temp[j].substring(0, 1).toUpperCase()).append(temp[j].substring(1).toLowerCase());
				}
			}
			list.add(sb.toString().toLowerCase());
		}
		return list;
	}

	/**
	 * This Method sets Excel Header row, sets font to bold and background color
	 * to Yellow
	 */
	private void addHeaderToExcel() {
		// XSSFFont xfont = workbook.createFont();
		// xfont.setBold(true);

		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("Table Name");
		header.createCell(1).setCellValue("Table Type From Cargo");
		header.createCell(2).setCellValue("Type From DB");
		header.createCell(3).setCellValue("Additional fields in Cargo");
		header.createCell(4).setCellValue("Additional fields in DB based of Cargo");
		header.createCell(5).setCellValue("Additional fields in DB based of GenDAO");
		header.createCell(6).setCellValue("Additional Mapping fields From GenDAO");
		header.createCell(7).setCellValue("Extendended by Cargo");
		header.createCell(8).setCellValue("Implemented by Cargo");
		header.createCell(9).setCellValue("Not Setter/Getter Methods");
		// CellStyle rs = header.getRowStyle();
		// rs.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		// rs.setFont(xfont);

	}

	/**
	 * This Method will write all the data into POI Excel worksheet Objects
	 * before writing into a file and then call a method that writes these
	 * objects into a physical file at given location
	 * 
	 * @param tab1List
	 *            - A List holding list of strings. each inner list is a column
	 *            value.
	 * @param error2Tab
	 *            - A list of all the Input table names for which an equivalent
	 *            cargo file is not found in the project.
	 */
	private void writeToExcelObject(List<List<String>> tab1List, List<String> error2Tab) {
		if (tab1List != null) {
			CellStyle cs = workbook.createCellStyle();

			cs.setWrapText(true);
			int rowNum = 1;
			for (List<String> tab1Content : tab1List) {
				Row row = sheet.createRow(rowNum++);
				int colNum = 0;
				for (Object eachCell : tab1Content) {
					Cell cell = row.createCell(colNum++);
					cell.setCellStyle(cs);
					if (eachCell instanceof String)
						cell.setCellValue("\n" + (String) eachCell);
				}
			}
		}
		// System.out.println(error2Tab);
		if (error2Tab != null) {
			int rowNum1 = 0;
			for (String tab2Content : error2Tab) {
				Row row = errorTab.createRow(rowNum1++);
				Cell cell = row.createCell(0);
				if (tab2Content instanceof String)
					cell.setCellValue((String) tab2Content);
			}
		}
		writeToExcelFile();
	}

	/**
	 * This method writes data from POI objects into excel file. If the file its
	 * writing into is open/read only will throws an exception
	 */
	private void writeToExcelFile() {
		try {
			FileOutputStream outputStream = new FileOutputStream("CompareExcelOutput.xlsx");
			FileOutputStream outputStreamError = new FileOutputStream("ErrorSheet.xlsx");
			workbookErrors.write(outputStreamError);
			workbook.write(outputStream);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Exception: Please close open Excel files, Cant write into file as it is in use");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				workbook.close();
				workbookErrors.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Data is formated and stored into a List<String> which intern is stored
	 * into an another list which will be later written into excel file
	 * 
	 * @param aMap2
	 */
	private void propareForXls(Map<String, Object> aMap2) {
		String Cargo_Additional_Fields = aMap2.get("Cargo_Additional_Fields").toString().replaceAll("[\\[\\]]", "")
				.replaceAll(",", ", \n");
		String Table_Additional_column = aMap2.get("Table_Additional_column").toString().replaceAll("[\\[\\]]", "")
				.replaceAll(",", ", \n");
		String Additional_fields_in_DB_based_of_GenDAO = aMap2.get("Additional_fields_in_DB_based_of_GenDAO").toString()
				.replaceAll("[\\[\\]]", "").replaceAll(",", ", \n");
		String Additional_Mapping_fields_GenDAO = aMap2.get("Additional_Mapping_fields_GenDAO").toString()
				.replaceAll("[\\[\\]]", "").replaceAll(",", ", \n");
		String Cargo_Implements = aMap2.get("Cargo_Implements").toString().replaceAll("[\\[\\]]", "").replaceAll(",",
				", \n");
		String Other_Methods = aMap2.get("Other_Methods").toString().replaceAll("[\\[\\]]", "").replaceAll(",", ", \n");

		// System.out.println(Cargo_Additional_Fields);
		// System.out.println(Other_Methods);

		tab1Content = new ArrayList<String>();
		tab1Content.add(aMap2.get("Table_Name").toString());
		//System.out.println(aMap2.get("Table_Name").toString());
		tab1Content.add(aMap2.get("Table_Type_As_Per_Cargo").toString());
		if (aMap2.get("Table_Type_As_Per_DB") == null) {
			tab1Content.add("Null");
		} else {
			tab1Content.add(aMap2.get("Table_Type_As_Per_DB").toString());
		}
		tab1Content.add(Cargo_Additional_Fields);
		tab1Content.add(Table_Additional_column);
		tab1Content.add(Additional_fields_in_DB_based_of_GenDAO);
		tab1Content.add(Additional_Mapping_fields_GenDAO);
		tab1Content.add(aMap2.get("Extends").toString());
		tab1Content.add(Cargo_Implements);
		tab1Content.add(Other_Methods);
		tab1List.add(tab1Content);
	}

	/**
	 * This is a registry of all the know packages where cargo files might be in
	 * the project.
	 * 
	 * @param pkgs2
	 *            - An empty list object to store package Names;
	 */
	private void populatePkgsForCargos(List<String> pkgs2) {
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.alert");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.appeal");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.application");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.appointment");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.audit");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.benefit");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.calendar");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.casereads");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.cases");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.conversion");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.correspondence");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.dcutil");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.dcwrapup");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.driver");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.electronicdocument");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.eligibility");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.employee");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.employeeschedule");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.ib");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.individual");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.individualfinancial");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.individualnonfinancial");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.individualresource");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.interestlist");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.interfaces");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.interimconvert");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.iq");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.mci");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.meetingroom");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.miscellaneous");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.office");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.prescreener");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.providermanagement");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.qualitycontrol");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.receptionlog");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.recovery");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.referencetable");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.report");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.reportqueue");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.rmc");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.security");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.selfservice");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.sp");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.standbylist");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.tanfredirect");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.tera");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.ucl");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.unit");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.webservices");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.wlredistribution");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.workassignment");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.workloadassignment");
		pkgs2.add("us.mi.state.dhs.bridges.business.entities.workloadrealignment");
		pkgs2.add("us.mi.state.dhs.fw.business.entities");

	}

	/**
	 * This is a registry of all the know packages where GenDAO files might be
	 * in the project.
	 * 
	 * @param genDAOPkgs2
	 *            - An empty list object to store package names
	 */
	private void populatePkgsForGenDAOs(List<String> genDAOPkgs2) {
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.alert");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.appeal");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.application");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.appointment");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.audit");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.benefit");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.calendar");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.casereads");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.cases");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.conversion");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.correspondence");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.dcwrapup");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.driver");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.electronicdocument");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.eligibility");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.employee");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.employeeschedule");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.ib");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.individual");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.individualfinancial");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.individualnonfinancial");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.individualresource");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.interestlist");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.interfaces");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.interimconvert");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.iq");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.mci");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.meetingroom");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.miscellaneous");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.office");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.prescreener");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.providermanagement");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.qualitycontrol");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.receptionlog");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.recovery");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.referencetable");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.report");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.reportqueue");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.rmc");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.security");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.selfservice");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.sp");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.standbylist");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.tanfredirect");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.tera");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.ucl");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.ucl.util");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.unit");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.webservices");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.wlredistribution");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.workassignment");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.workloadassignment");
		genDAOPkgs2.add("us.mi.state.dhs.bridges.data.oracle.workloadrealignment");
		genDAOPkgs2.add("us.mi.state.dhs.fw.data.oracle");
	}

	/**
	 * This method prints data in Given Map into File
	 * 
	 * @param aMap
	 *            - Holds data that needs to be print into file
	 * @deprecated
	 */

	/*
	 * private void writeToFile(Map<String, Object> aMap) {
	 * 
	 * counter++; writer.println("=============START================");
	 * writer.println(counter + ") Table Name: " + aMap.get("Table_Name") +
	 * "\n"); writer.println("Fields that are not in Table:" +
	 * aMap.get("Cargo_Additional_Fields") + "\n");
	 * writer.println("Table Columns that are not in Cargo" +
	 * aMap.get("Table_Additional_column") + "\n");
	 * writer.println("############END##################\n");
	 * 
	 * }
	 */
	/**
	 * This method when given an cargo returns list of all fields from cargo and
	 * its extended classes
	 * 
	 * @param listOfFieldsFromCargo
	 *            - An empty list object to store fields information
	 * @param cargo
	 *            - Class instance of cargo file
	 */
	private void getFieldsAndTabeTypeFromCargoExtendedClass(List<String> listOfFieldsFromCargo, Class<?> cargo) {
		if ("AuditType0Cargo".equalsIgnoreCase(cargo.getSuperclass().getSimpleName().toString())) {
			aMap.put("Table_Type_As_Per_Cargo", "0");
			aMap.put("Extends", "");
			populateAuditType0ValuesIntoList(listOfFieldsFromCargo);
		} else if ("AuditType1Cargo".equalsIgnoreCase(cargo.getSuperclass().getSimpleName().toString())) {
			aMap.put("Table_Type_As_Per_Cargo", "1");
			aMap.put("Extends", "");
			populateAuditType1ValuesIntoList(listOfFieldsFromCargo);
		} else if ("AuditType2Cargo".equalsIgnoreCase(cargo.getSuperclass().getSimpleName().toString())) {
			aMap.put("Table_Type_As_Per_Cargo", "2");
			aMap.put("Extends", "");
			populateAuditType2ValuesIntoList(listOfFieldsFromCargo);
		} else {
			if (hasSuperclass(cargo)) {
				if (hasSuperclass(cargo.getSuperclass())) {
					if ("AuditType0Cargo"
							.equalsIgnoreCase(cargo.getSuperclass().getSuperclass().getSimpleName().toString())) {
						aMap.put("Table_Type_As_Per_Cargo", "0");
						aMap.put("Extends", cargo.getSuperclass().getSimpleName().toString());
						populateAuditType0ValuesIntoList(listOfFieldsFromCargo);

						System.out.println("This " + cargo.getSimpleName() + " cargo extends a custom cargo: "
								+ cargo.getSuperclass().getSimpleName().toString());
					} else if ("AuditType1Cargo"
							.equalsIgnoreCase(cargo.getSuperclass().getSuperclass().getSimpleName().toString())) {
						aMap.put("Table_Type_As_Per_Cargo", "1");
						aMap.put("Extends", cargo.getSuperclass().getSimpleName().toString());
						populateAuditType1ValuesIntoList(listOfFieldsFromCargo);

						System.out.println("This " + cargo.getSimpleName() + " cargo extends a custom cargo: "
								+ cargo.getSuperclass().getSimpleName().toString());
					} else if ("AuditType2Cargo"
							.equalsIgnoreCase(cargo.getSuperclass().getSuperclass().getSimpleName().toString())) {
						aMap.put("Table_Type_As_Per_Cargo", "2");
						aMap.put("Extends", cargo.getSuperclass().getSimpleName().toString());
						populateAuditType2ValuesIntoList(listOfFieldsFromCargo);

						System.out.println("This " + cargo.getSimpleName() + " cargo extends a custom cargo: "
								+ cargo.getSuperclass().getSimpleName().toString());
					} else
						aMap.put("Table_Type_As_Per_Cargo", "Error");
				} else {
					aMap.put("Table_Type_As_Per_Cargo", "Unknown");
					aMap.put("Extends", cargo.getSuperclass().getSimpleName().toString());
					System.out.println("This " + cargo.getSimpleName() + " cargo extends a custom cargo: "
							+ cargo.getSuperclass().getSimpleName().toString());
				}

			}
		}

	}

	/**
	 * Type 2 table constant fields
	 * 
	 * @param listOfFieldsFromCargo
	 *            - a list
	 */
	private void populateAuditType2ValuesIntoList(List<String> listOfFieldsFromCargo) {
		listOfFieldsFromCargo.add("audituserid");
		listOfFieldsFromCargo.add("auditdt");
		listOfFieldsFromCargo.add("updateuserid");
		listOfFieldsFromCargo.add("updatedt");
		listOfFieldsFromCargo.add("histnavind");
		listOfFieldsFromCargo.add("effbegindt");
		listOfFieldsFromCargo.add("effenddt");
		listOfFieldsFromCargo.add("verfreceiveddt");
		listOfFieldsFromCargo.add("discoverydt");
		listOfFieldsFromCargo.add("reportdt");
		listOfFieldsFromCargo.add("historyseq");
		listOfFieldsFromCargo.add("ssdatachanged");
		listOfFieldsFromCargo.add("ssdisplaymessage");
		listOfFieldsFromCargo.add("archivedt");
		listOfFieldsFromCargo.add("uniquetransid");
		listOfFieldsFromCargo.add("voidsw");
		listOfFieldsFromCargo.add("validator");
		listOfFieldsFromCargo.add("user");
		listOfFieldsFromCargo.add("id");
		listOfFieldsFromCargo.add("dirty");
		listOfFieldsFromCargo.add("isnew");
		listOfFieldsFromCargo.add("deleted");
		listOfFieldsFromCargo.add("createdt");
		listOfFieldsFromCargo.add("createuserid");
		listOfFieldsFromCargo.add("updatedt");
		listOfFieldsFromCargo.add("updateuserid");
	}

	/**
	 * Type 1 table constant fields
	 * 
	 * @param listOfFieldsFromCargo
	 *            - A list
	 */
	private void populateAuditType1ValuesIntoList(List<String> listOfFieldsFromCargo) {
		listOfFieldsFromCargo.add("audituserid");
		listOfFieldsFromCargo.add("auditdt");
		listOfFieldsFromCargo.add("historyseq");
		listOfFieldsFromCargo.add("ssdatachanged");
		listOfFieldsFromCargo.add("ssdisplaymessage");
		listOfFieldsFromCargo.add("archivedt");
		listOfFieldsFromCargo.add("uniquetransid");
		listOfFieldsFromCargo.add("voidsw");
		listOfFieldsFromCargo.add("validator");
		listOfFieldsFromCargo.add("user");
		listOfFieldsFromCargo.add("id");
		listOfFieldsFromCargo.add("dirty");
		listOfFieldsFromCargo.add("isnew");
		listOfFieldsFromCargo.add("deleted");
		listOfFieldsFromCargo.add("createdt");
		listOfFieldsFromCargo.add("createuserid");
		listOfFieldsFromCargo.add("updatedt");
		listOfFieldsFromCargo.add("updateuserid");
	}

	/**
	 * Type 0 table constant fields
	 * 
	 * @param listOfFieldsFromCargo
	 *            - A list
	 */
	private void populateAuditType0ValuesIntoList(List<String> listOfFieldsFromCargo) {
		listOfFieldsFromCargo.add("ssdatachanged");
		listOfFieldsFromCargo.add("ssdisplaymessage");
		listOfFieldsFromCargo.add("archivedt");
		listOfFieldsFromCargo.add("uniquetransid");
		listOfFieldsFromCargo.add("voidsw");
		listOfFieldsFromCargo.add("validator");
		listOfFieldsFromCargo.add("user");
		listOfFieldsFromCargo.add("id");
		listOfFieldsFromCargo.add("dirty");
		listOfFieldsFromCargo.add("isnew");
		listOfFieldsFromCargo.add("deleted");
		listOfFieldsFromCargo.add("createdt");
		listOfFieldsFromCargo.add("createuserid");
		listOfFieldsFromCargo.add("updatedt");
		listOfFieldsFromCargo.add("updateuserid");
	}

	/**
	 * Given a table returns it Cargo and GenDAO names.
	 * 
	 * @param line
	 * @param cargoGenSW
	 *            - 'C' if cargo, 'G' - if GenDAO
	 * @return returns Cargo/GenDAO class name including package Name.
	 */
	private String getQualifiedName(String line, char cargoGenSW) {

		String[] temp = null;
		StringBuilder st = new StringBuilder();
		temp = line.split("_");
		try {
			for (int i = 0; i < temp.length; i++) {
				if (Character.isDigit(temp[i].charAt(0))) {
					char[] tempNum = temp[i].toCharArray();
					boolean isFirstOcc = true;
					for (int j = 0; j < tempNum.length; j++) {
						if (Character.isDigit(tempNum[j]))
							st.append(tempNum[j]);
						else {
							if (isFirstOcc) {
								st.append(Character.toUpperCase(tempNum[j]));
								isFirstOcc = false;
							} else {
								st.append(Character.toLowerCase(tempNum[j]));
							}
						}
					}
				} else {
					st.append(temp[i].substring(0, 1).toUpperCase()).append(temp[i].substring(1).toLowerCase());
				}
			}
		} catch (StringIndexOutOfBoundsException e) {
			System.out.println("Inside method getQualifiedName(): " + line);
		}
		if (cargoGenSW == 'C') {
			st.append("Cargo");
			for (String pk : cargoPkgs) {
				if (isClass(pk + "." + st.toString())) {
					return pk + "." + st.toString();
				}
			}
		} else if (cargoGenSW == 'G') {
			st.append("GenDAO");
			for (String pk : GenDAOPkgs) {
				if (isClass(pk + "." + st.toString())) {
					return pk + "." + st.toString();
				}
			}
		}
		return null;
	}

	/**
	 * This Method list all the column names and the table Type by querying
	 * Database
	 * 
	 * @param line
	 *            - Table Name
	 * @return - list of all the column Names
	 */
	private List<String> getTableTypeAndColumnNamesFromDB(String line) {
		StringBuilder SQL_QUERY = new StringBuilder("select A.*, A.rowid from ").append(line).append(" A");
		StringBuilder DB_TABLE = new StringBuilder(
				"select B.db_table_type_ind from DB_TABLES B where B.TABLE_NAME = :1");
		List<String> colNames = new ArrayList<String>();
		String[] temp;
		StringBuilder sb = null;
		try {
			PreparedStatement stm = null;
			ResultSet result = null;
			stm = con.prepareStatement(DB_TABLE.toString());
			stm.setString(1, line);
			result = stm.executeQuery();
			if (result.next()) {
				aMap.put("Table_Type_As_Per_DB", result.getString("DB_TABLE_TYPE_IND"));
			} else {
				aMap.put("Table_Type_As_Per_DB", "ResultSet Empty");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			PreparedStatement statement = null;
			ResultSet rs = null;
			ResultSetMetaData rsmd = null;
			statement = con.prepareStatement(SQL_QUERY.toString());
			rs = statement.executeQuery();
			rsmd = rs.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				sb = new StringBuilder();
				temp = rsmd.getColumnName(i).split("_");
				for (int j = 0; j < temp.length; j++) {
					if (j == 0) {
						sb.append(temp[j].toLowerCase());
					} else {
						sb.append(temp[j].substring(0, 1).toUpperCase()).append(temp[j].substring(1).toLowerCase());
					}
				}
				colNames.add(sb.toString().toLowerCase());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return colNames;
	}

	/**
	 * This method takes in a class name as sting and returns it object
	 * 
	 * @param cargoQualifiedName
	 *            - Object of class
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static Class<?> getClassInstanceForATable(String cargoQualifiedName) throws ClassNotFoundException {
		return Class.forName(cargoQualifiedName);
	}

	/**
	 * This method takes in a class name and returns all the fields in it as a
	 * list<strings>
	 * 
	 * @param c
	 *            - Class Object to identify field names
	 * @return - list of all the field names
	 */
	public Function<Class<?>, List<String>> listAllFieldNames = c -> {
		List<String> lst = new ArrayList<String>();
		Field[] fields = this.listAllFields.apply(c);
		for (Field field : fields)
			lst.add(field.getName().toLowerCase());
		return lst;
	};

	/**
	 * This method takes in a class name and returns all the fields in it as a
	 * Fields[]
	 * 
	 * @param c
	 *            - Class Object to identify field names
	 * @return - java.lang.reflect.Field[] of all the field names
	 */
	public Function<Class<?>, Field[]> listAllFields = c -> {
		return c.getDeclaredFields();
	};

	/**
	 * This method takes in a class name as string and check whether it is a
	 * class or not
	 * 
	 * @param className
	 * @return true if class
	 */
	public boolean isClass(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * takes in a class Name and checks if it has a superclass
	 * 
	 * @param cargo
	 *            - class
	 * @return true is it does
	 */
	public boolean hasSuperclass(Class<?> cargo) {
		try {
			cargo.getSuperclass();
			return true;
		} catch (Exception e) {
			System.out.print("exception inside hasSuperClass: " + e);
		}
		return false;
	}

	/**
	 * Method take a class and returns all the methods in it that are not setter
	 * and getters of fields and puts them into a map
	 * 
	 * @param cargo
	 *            - class
	 */
	public void notSetGetMethods(Class<?> cargo) {
		List<String> allMethods = new ArrayList<String>();

		Method[] tempMethods = listAllMethods.apply(cargo);
		for (Method m : tempMethods) {
			allMethods.add(m.getName().toLowerCase().toString());
		}

		allMethods.removeAll(allGetterMethodsBasedOfFieldsInClass);
		allMethods.removeAll(allSetterMethodsBasedOfFieldsInClass);

		aMap.put("Other_Methods", allMethods);
	}

	/**
	 * Method <i>listAllSetters</i> provides with an array of all Methods
	 * (java.lang.reflect.Method) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>Method[]</tt> A java.lang.reflect.Method[] containing all the
	 *         method.
	 */
	public Function<Class<?>, Method[]> listAllMethods = c -> {
		return c.getDeclaredMethods();
	};

	/**
	 * Method <i>isGetter</i> checks whether a method is a getter method of a
	 * class based on the following rule:
	 * <p>
	 * A getter method have its name start with "get" or "is", take 0
	 * parameters, and returns a value.
	 * </p>
	 * 
	 * @param m
	 *            The parent pathname java.lang.reflect.Method
	 * @return <tt>true</tt> if this Method is a Getter Method
	 */
	public Function<Method, Boolean> isGetter = m -> {
		if (!(m.getName().startsWith("get") || m.getName().startsWith("is")))
			return false;
		if (m.getParameterTypes().length != 0)
			return false;
		if (void.class.equals(m.getReturnType()))
			return false;
		return true;
	};

	/**
	 * Method <i>isSetter</i> checks whether a method is a setter method of a
	 * class based on the following rule:
	 * <p>
	 * A setter method have its name start with "set", and takes 1 parameter.
	 * </p>
	 * 
	 * @param m
	 *            The parent pathname java.lang.reflect.Method
	 * @return <tt>true</tt> if this Method is a Setter Method
	 */
	public Function<Method, Boolean> isSetter = m -> {
		if (!m.getName().startsWith("set"))
			return false;
		if (m.getParameterTypes().length != 1)
			return false;
		return true;
	};
}
