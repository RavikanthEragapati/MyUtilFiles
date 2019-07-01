package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONDisplay {

	public static Set<String> columnHeader = new HashSet<>();
	public static Map<String, Object> parsedJson = null;
	public static List<Map<String, Object>> parsedEndData = null;

	public static void main(String[] args) {

		try {
			String json1 = null;
			String json2 = null;
			String json3 = null;

			JSONObject jsonObj1 = null;
			JSONObject jsonObj2 = null;
			JSONObject jsonObj3 = null;

			FileInputStream file = new FileInputStream(
					new File("C:\\Workspace\\Bridges_Workspace\\Test\\src\\com\\test\\input.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			Sheet sheet = workbook.getSheetAt(0);
			StringBuilder sb = null;
			parsedEndData = new ArrayList<Map<String, Object>>();
			for (Row row : sheet) {
				parsedJson = new HashMap<String, Object>();
				for (Cell cell : row) {
					if (cell.getColumnIndex() == 0) {
						json1 = cell.getStringCellValue();
						jsonObj1 = new JSONObject(json1.trim());
					} else if (cell.getColumnIndex() == 1) {

						parsedJson.put("TransactionID", cell.getStringCellValue());
						columnHeader.add("TransactionID");
						// json2 = cell.getStringCellValue();
						// jsonObj2 = new JSONObject(json2);
					} else if (cell.getColumnIndex() == 2) {
						json3 = cell.getStringCellValue();
						jsonObj3 = new JSONObject(json3);
					}
				}
				if (jsonObj1 != null) {
					method(jsonObj1);
				}
				if (jsonObj3 != null) {
					method(jsonObj3);
				}
				parsedEndData.add(parsedJson);
				// columnHeader.forEach(a -> {
				// System.out.println(a + ":" + parsedJson.get(a));
				// });
				//
				// System.out.println("#####################################");
			}

			writeToExcel();

		} catch (IOException | EncryptedDocumentException | JSONException e) {
			e.printStackTrace();
		}
	}

	private static void writeToExcel() throws IOException {
		FileOutputStream outputStream = new FileOutputStream("JSONExcel.xlsx");
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("Output");

		Map<String, Integer> headerIndex = addHeaderToExcel(sheet);

		for (int rowIndex = 0; rowIndex < parsedEndData.size(); rowIndex++) {
			Row data = sheet.createRow(rowIndex + 1);
			Map<String, Object> aMap = parsedEndData.get(rowIndex);

			for (Map.Entry<String, Object> entry : aMap.entrySet()) {
				data.createCell(headerIndex.get(entry.getKey())).setCellValue(entry.getValue() + "");

				// System.out.println("Key = " + entry.getKey() + ", Value = " +
				// entry.getValue());
			}

		}
		workbook.write(outputStream);
		workbook.close();
	}

	private static Map<String, Integer> addHeaderToExcel(XSSFSheet sheet) {
		Map<String, Integer> headerIndex = new HashMap<String, Integer>();
		Row header = sheet.createRow(0);
		int index = 0;
		Iterator<String> itr = columnHeader.iterator();
		while (itr.hasNext()) {
			header.createCell(index).setCellValue(itr.next());
			headerIndex.put(header.getCell(index).getStringCellValue(), index);
			index++;
		}
		return headerIndex;
	}

	private static void method(JSONObject jsonObj1) {
		JSONObject tempObject = null;
		JSONArray tempArray = null;
		String key = null;
		try {

			Iterator<String> json1Keys = jsonObj1.keys();
			while (json1Keys.hasNext()) {

				key = json1Keys.next();

				if (jsonObj1.get(key) instanceof JSONObject) {

					tempObject = (JSONObject) jsonObj1.get(key);
					method(tempObject);
				} else if (jsonObj1.get(key) instanceof JSONArray) {
					tempArray = (JSONArray) jsonObj1.get(key);
					methodLoopArray(tempArray, key);

				} else if (jsonObj1.get(key) instanceof String) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {

						parsedJson.put(key, parsedJson.get(key) + ",\n " + (String) jsonObj1.get(key));
					} else
						parsedJson.put(key, (String) jsonObj1.get(key));
					// System.out.println(key + " : " + (String)
					// jsonObj1.get(key));
				} else if (jsonObj1.get(key) instanceof Boolean) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {
						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Boolean) jsonObj1.get(key));
					} else
						parsedJson.put(key, (Boolean) jsonObj1.get(key));
					// System.out.println(key + " : " + (Boolean)
					// jsonObj1.get(key));
				} else if (jsonObj1.get(key) instanceof Integer) {
					columnHeader.add(key);

					if (parsedJson.containsKey(key)) {
						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Integer) jsonObj1.get(key));
					} else
						parsedJson.put(key, (Integer) jsonObj1.get(key));
					// System.out.println(key + " : " + (Integer)
					// jsonObj1.get(key));
				} else if (jsonObj1.get(key) instanceof Long) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {
						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Long) jsonObj1.get(key));
					} else
						parsedJson.put(key, (Long) jsonObj1.get(key));
					System.out.println(key + " : " + (Long) jsonObj1.get(key));
				} else {
					if (jsonObj1.get(key) == JSONObject.NULL) {
						// System.out.println(key + " : " + null);
						columnHeader.add(key);
						if (parsedJson.containsKey(key)) {
							parsedJson.put(key, parsedJson.get(key) + ",\n " + null);
						} else
							parsedJson.put(key, null);
					}
				}

			}
		} catch (EncryptedDocumentException | JSONException e) {
			e.printStackTrace();
		}
	}

	private static void methodLoopArray(JSONArray tempArray, String key) {
		JSONObject tempObj = null;
		JSONArray tempArray1 = null;
		for (int i = 0; i < tempArray.length(); i++) {
			try {
				Object jsonObject = tempArray.get(i);
				if (jsonObject instanceof JSONObject) {
					method((JSONObject) jsonObject);
				} else if (jsonObject instanceof JSONArray) {
					methodLoopArray((JSONArray) jsonObject, key);
				} else if (jsonObject instanceof String) {

					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {

						parsedJson.put(key, parsedJson.get(key) + ",\n " + (String) jsonObject);
					} else
						parsedJson.put(key, (String) jsonObject);

					System.out.println((String) jsonObject);
				} else if (jsonObject instanceof Boolean) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {

						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Boolean) jsonObject);
					} else
						parsedJson.put(key, (Boolean) jsonObject);

					System.out.println(i + " : " + (Boolean) jsonObject);
				} else if (jsonObject instanceof Integer) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {

						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Integer) jsonObject);
					} else
						parsedJson.put(key, (Integer) jsonObject);

					System.out.println(i + " : " + (Integer) jsonObject);
				} else if (jsonObject instanceof Long) {
					columnHeader.add(key);
					if (parsedJson.containsKey(key)) {

						parsedJson.put(key, parsedJson.get(key) + ",\n " + (Long) jsonObject);
					} else
						parsedJson.put(key, (Long) jsonObject);

					System.out.println(i + " : " + (Long) jsonObject);
				} else {
					if (jsonObject == JSONObject.NULL) {

						columnHeader.add(key);
						if (parsedJson.containsKey(key)) {

							parsedJson.put(key, parsedJson.get(key) + ",\n " + "null");
						} else
							parsedJson.put(key, "null");

						System.out.println(i + " : " + null);
					}
				}

			} catch (EncryptedDocumentException | JSONException e) {
				e.printStackTrace();
			}

		}

	}
}
