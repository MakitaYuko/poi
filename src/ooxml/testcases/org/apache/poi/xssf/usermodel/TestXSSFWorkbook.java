/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.usermodel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.poi.hssf.HSSFTestDataSamples;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.XSSFTestDataSamples;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.openxml4j.opc.ContentTypes;
import org.apache.poi.openxml4j.opc.Package;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook;

public final class TestXSSFWorkbook extends TestCase {

	@Override
	protected void setUp() {
		// Use system out logger
		System.setProperty(
				"org.apache.poi.util.POILogger",
				"org.apache.poi.util.SystemOutLogger"
		);
	}

	public void testGetSetActiveSheet(){
		XSSFWorkbook workbook = new XSSFWorkbook();
        assertEquals(0, workbook.getActiveSheetIndex());

		workbook.createSheet("sheet1");
		workbook.createSheet("sheet2");
		workbook.createSheet("sheet3");
		// set second sheet
		workbook.setActiveSheet(1);
		// test if second sheet is set up
		assertEquals(1, workbook.getActiveSheetIndex());
	}

	public void testGetSheetIndex() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet1 = workbook.createSheet("sheet1");
		Sheet sheet2 = workbook.createSheet("sheet2");
		assertEquals(0, workbook.getSheetIndex(sheet1));
		assertEquals(0, workbook.getSheetIndex("sheet1"));
		assertEquals(1, workbook.getSheetIndex(sheet2));
		assertEquals(1, workbook.getSheetIndex("sheet2"));
		assertEquals(-1, workbook.getSheetIndex("noSheet"));
	}
	
	public void testSetSheetOrder() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet1 = workbook.createSheet("sheet1");
		Sheet sheet2 = workbook.createSheet("sheet2");
		assertSame(sheet1, workbook.getSheetAt(0));
		assertSame(sheet2, workbook.getSheetAt(1));
		workbook.setSheetOrder("sheet2", 0);
		assertSame(sheet2, workbook.getSheetAt(0));
		assertSame(sheet1, workbook.getSheetAt(1));
		// Test reordering of CTSheets
		CTWorkbook ctwb = workbook.getCTWorkbook();
		CTSheet[] ctsheets = ctwb.getSheets().getSheetArray();
		assertEquals("sheet2", ctsheets[0].getName());
		assertEquals("sheet1", ctsheets[1].getName());
		
		// Borderline case: only one sheet
		workbook = new XSSFWorkbook();
		sheet1 = workbook.createSheet("sheet1");
		assertSame(sheet1, workbook.getSheetAt(0));
		workbook.setSheetOrder("sheet1", 0);
		assertSame(sheet1, workbook.getSheetAt(0));
	}
	
	public void testSetSheetName() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.createSheet("sheet1");
		assertEquals("sheet1", workbook.getSheetName(0));
		workbook.setSheetName(0, "sheet2");
		assertEquals("sheet2", workbook.getSheetName(0));
	}
	
	public void testCloneSheet() {
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFSheet sheet = book.createSheet("TEST");
        sheet.createRow(0).createCell(0).setCellValue("Test");
        sheet.createRow(1).createCell(0).setCellValue(36.6);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 2));
        sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 2));
        assertTrue(sheet.isSelected());

        XSSFSheet clonedSheet = book.cloneSheet(0);
        assertEquals("TEST (2)", clonedSheet.getSheetName());
        assertEquals(2, clonedSheet.getPhysicalNumberOfRows());
        assertEquals(2, clonedSheet.getNumMergedRegions());
        assertFalse(clonedSheet.isSelected());

        //cloned sheet is a deep copy, adding rows in the original does not affect the clone
        sheet.createRow(2).createCell(0).setCellValue(1);
        sheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 2));
        assertEquals(2, clonedSheet.getPhysicalNumberOfRows());
        assertEquals(2, clonedSheet.getPhysicalNumberOfRows());

        clonedSheet.createRow(2).createCell(0).setCellValue(1);
        clonedSheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 2));
        assertEquals(3, clonedSheet.getPhysicalNumberOfRows());
        assertEquals(3, clonedSheet.getPhysicalNumberOfRows());

    }
	
	public void testGetSheetByName() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet1 = workbook.createSheet("sheet1");
		Sheet sheet2 = workbook.createSheet("sheet2");
		assertSame(sheet1, workbook.getSheet("sheet1"));
		assertSame(sheet2, workbook.getSheet("sheet2"));
		assertNull(workbook.getSheet("nosheet"));
	}
	
	public void testRemoveSheetAt() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.createSheet("sheet1");
		workbook.createSheet("sheet2");
		workbook.createSheet("sheet3");
		workbook.removeSheetAt(1);
		assertEquals(2, workbook.getNumberOfSheets());
		assertEquals("sheet3", workbook.getSheetName(1));
		workbook.removeSheetAt(0);
		assertEquals(1, workbook.getNumberOfSheets());
		assertEquals("sheet3", workbook.getSheetName(0));
		workbook.removeSheetAt(0);
		assertEquals(0, workbook.getNumberOfSheets());
	}

	public void testPrintArea(){
	 Workbook workbook = new XSSFWorkbook();
	 Sheet sheet = workbook.createSheet("Test Print Area");
	 String sheetName = workbook.getSheetName(0);
	 
	// String reference = sheetName+"!$A$1:$B$1";
	// workbook.setPrintArea(0, reference);
	 workbook.setPrintArea(0,1,5,4,9);
	 
	 String retrievedPrintArea = workbook.getPrintArea(0);

 	 //assertNotNull("Print Area not defined for first sheet", retrievedPrintArea);
	 assertEquals("'"+sheetName+"'!$B$5:$F$10", retrievedPrintArea);
}
	
	public void testRepeatingRowsAndColums() {
		// First test that setting RR&C for same sheet more than once only creates a 
		// single  Print_Titles built-in record
		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet("FirstSheet");
		
		// set repeating rows and columns twice for the first sheet
		for (int i = 0; i < 2; i++) {
			wb.setRepeatingRowsAndColumns(0, 0, 0, 0, 3);
			//sheet.createFreezePane(0, 3);
		}
		assertEquals(1, wb.getNumberOfNames());
		XSSFName nr1 = wb.getNameAt(0);
		
		assertEquals(XSSFName.BUILTIN_PRINT_TITLE, nr1.getNameName());
		assertEquals("'FirstSheet'!$A:$A,'FirstSheet'!$1:$4", nr1.getRefersToFormula());
		
		// Save and re-open
		XSSFWorkbook nwb = XSSFTestDataSamples.writeOutAndReadBack(wb);

		assertEquals(1, nwb.getNumberOfNames());
		nr1 = nwb.getNameAt(0);
		
		assertEquals(XSSFName.BUILTIN_PRINT_TITLE, nr1.getNameName());
		assertEquals("'FirstSheet'!$A:$A,'FirstSheet'!$1:$4", nr1.getRefersToFormula());
		
		// check that setting RR&C on a second sheet causes a new Print_Titles built-in
		// name to be created
		sheet = nwb.createSheet("SecondSheet");
		nwb.setRepeatingRowsAndColumns(1, 1, 2, 0, 0);

		assertEquals(2, nwb.getNumberOfNames());
		XSSFName nr2 = nwb.getNameAt(1);
		
		assertEquals(XSSFName.BUILTIN_PRINT_TITLE, nr2.getNameName());
		assertEquals("'SecondSheet'!$B:$C,'SecondSheet'!$1:$1", nr2.getRefersToFormula());
		
		
		nwb.setRepeatingRowsAndColumns(1, -1, -1, -1, -1);

	}
	

	/**
	 * Tests that we can save a new document
	 */
	public void testSaveNew() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.createSheet("sheet1");
		workbook.createSheet("sheet2");
		workbook.createSheet("sheet3");

        XSSFTestDataSamples.writeOutAndReadBack(workbook);
	}

	/**
	 * Tests that we can save, and then re-load a new document
	 */
	public void testSaveLoadNew() throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet1 = workbook.createSheet("sheet1");
		Sheet sheet2 = workbook.createSheet("sheet2");
		workbook.createSheet("sheet3");
		
		RichTextString rts = workbook.getCreationHelper().createRichTextString("hello world");
		
		sheet1.createRow(0).createCell((short)0).setCellValue(1.2);
		sheet1.createRow(1).createCell((short)0).setCellValue(rts);
		sheet2.createRow(0);
		
		assertEquals(0, workbook.getSheetAt(0).getFirstRowNum());
		assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
		assertEquals(0, workbook.getSheetAt(1).getFirstRowNum());
		assertEquals(0, workbook.getSheetAt(1).getLastRowNum());
		assertEquals(-1, workbook.getSheetAt(2).getFirstRowNum());
		assertEquals(-1, workbook.getSheetAt(2).getLastRowNum());
		
		File file = File.createTempFile("poi-", ".xlsx");
		OutputStream out = new FileOutputStream(file);
		workbook.write(out);
		out.close();
		
		// Check the package contains what we'd expect it to
		Package pkg = Package.open(file.toString());
		PackagePart wbRelPart = 
			pkg.getPart(PackagingURIHelper.createPartName("/xl/_rels/workbook.xml.rels"));
		assertNotNull(wbRelPart);
		assertTrue(wbRelPart.isRelationshipPart());
		assertEquals(ContentTypes.RELATIONSHIPS_PART, wbRelPart.getContentType());
		
		PackagePart wbPart = 
			pkg.getPart(PackagingURIHelper.createPartName("/xl/workbook.xml"));
		// Links to the three sheets, shared strings and styles
		assertTrue(wbPart.hasRelationships());
		assertEquals(5, wbPart.getRelationships().size());
		
		// Load back the XSSFWorkbook
		workbook = new XSSFWorkbook(pkg);
		assertEquals(3, workbook.getNumberOfSheets());
		assertNotNull(workbook.getSheetAt(0));
		assertNotNull(workbook.getSheetAt(1));
		assertNotNull(workbook.getSheetAt(2));
		
		assertNotNull(workbook.getSharedStringSource());
		assertNotNull(workbook.getStylesSource());
		
		assertEquals(0, workbook.getSheetAt(0).getFirstRowNum());
		assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
		assertEquals(0, workbook.getSheetAt(1).getFirstRowNum());
		assertEquals(0, workbook.getSheetAt(1).getLastRowNum());
		assertEquals(-1, workbook.getSheetAt(2).getFirstRowNum());
		assertEquals(-1, workbook.getSheetAt(2).getLastRowNum());
		
		sheet1 = workbook.getSheetAt(0);
		assertEquals(1.2, sheet1.getRow(0).getCell(0).getNumericCellValue(), 0.0001);
		assertEquals("hello world", sheet1.getRow(1).getCell(0).getRichStringCellValue().getString());
	}
	
	public void testExisting() throws Exception {
		
		XSSFWorkbook workbook = XSSFTestDataSamples.openSampleWorkbook("Formatting.xlsx");
		assertNotNull(workbook.getSharedStringSource());
		assertNotNull(workbook.getStylesSource());
		
		// And check a few low level bits too
		Package pkg = Package.open(HSSFTestDataSamples.openSampleFileStream("Formatting.xlsx"));
		PackagePart wbPart = 
			pkg.getPart(PackagingURIHelper.createPartName("/xl/workbook.xml"));
		
		// Links to the three sheets, shared, styles and themes
		assertTrue(wbPart.hasRelationships());
		assertEquals(6, wbPart.getRelationships().size());

	}
	
	public void testFindFont(){
		//get default font and check against default value
		XSSFWorkbook workbook = new XSSFWorkbook();
		Font fontFind=workbook.findFont(Font.BOLDWEIGHT_NORMAL, IndexedColors.BLACK.getIndex(), (short)11, "Calibri", false, false, Font.SS_NONE, Font.U_NONE);
		assertNotNull(fontFind);
		
		//get default font, then change 2 values and check against different values (height changes)
		Font font=workbook.createFont();
		((XSSFFont)font).setBold(true);
		font.setUnderline(Font.U_DOUBLE);
		fontFind=workbook.findFont(Font.BOLDWEIGHT_BOLD, IndexedColors.BLACK.getIndex(), (short)15, "Calibri", false, false, Font.SS_NONE, Font.U_DOUBLE);
		assertNull(fontFind);
	}

	public void testGetCellStyleAt(){
	 	XSSFWorkbook workbook = new XSSFWorkbook();
		short i = 0;
		//get default style
		CellStyle cellStyleAt = workbook.getCellStyleAt(i);
		assertNotNull(cellStyleAt);
		
		//get custom style
		StylesTable styleSource = workbook.getStylesSource();
		XSSFCellStyle customStyle = new XSSFCellStyle(styleSource);
		XSSFFont font = new XSSFFont();
		font.setFontName("Verdana");
		customStyle.setFont(font);
		int x = styleSource.putStyle(customStyle);
		cellStyleAt = workbook.getCellStyleAt((short)x);
		assertNotNull(cellStyleAt);		
	}
	
	public void testGetFontAt(){
	 	XSSFWorkbook workbook = new XSSFWorkbook();
		StylesTable styleSource = workbook.getStylesSource();
		short i = 0;
		//get default font
		Font fontAt = workbook.getFontAt(i);
		assertNotNull(fontAt);
		
		//get customized font
		XSSFFont customFont = new XSSFFont();
		customFont.setItalic(true);
		int x = styleSource.putFont(customFont);
		fontAt = workbook.getFontAt((short)x);
		assertNotNull(fontAt);
	}
	
	public void testGetNumberOfFonts(){
	 	XSSFWorkbook wb = new XSSFWorkbook();

		XSSFFont f1=wb.createFont();
	 	f1.setBold(true);
	 	wb.createCellStyle().setFont(f1);

		XSSFFont f2=wb.createFont();
	 	f2.setUnderline(Font.U_DOUBLE);
		wb.createCellStyle().setFont(f2);

		XSSFFont f3=wb.createFont();
	 	f3.setFontHeightInPoints((short)23);
		wb.createCellStyle().setFont(f3);

		assertEquals(4,wb.getNumberOfFonts());
	 	assertEquals(Font.U_DOUBLE,wb.getFontAt((short)2).getUnderline());
	}
	
	public void testGetNumCellStyles(){
	 	XSSFWorkbook workbook = new XSSFWorkbook();
		short i = workbook.getNumCellStyles();
		//get default cellStyles
		assertEquals(1, i);
		//get wrong value
		assertNotSame(2, i);		
	}
	
	public void testGetDisplayedTab(){
		XSSFWorkbook workbook = new XSSFWorkbook();
		short i = (short) workbook.getFirstVisibleTab();
		//get default diplayedTab
		assertEquals(0, i);		
	}
	
	public void testSetDisplayedTab(){
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.setFirstVisibleTab(1);
		short i = (short) workbook.getFirstVisibleTab();
		//0 (defualt value) is not longer set
		assertNotSame(0, i);
		//1 is the default tab
		assertEquals(1, i);
	}
	
	
	public void testLoadSave() {
		XSSFWorkbook workbook = XSSFTestDataSamples.openSampleWorkbook("Formatting.xlsx");
		assertEquals(3, workbook.getNumberOfSheets());
		assertEquals("dd/mm/yyyy", workbook.getSheetAt(0).getRow(1).getCell(0).getRichStringCellValue().getString());
		assertNotNull(workbook.getSharedStringSource());
		assertNotNull(workbook.getStylesSource());
		
		// Write out, and check
		// Load up again, check all still there
		XSSFWorkbook wb2 = XSSFTestDataSamples.writeOutAndReadBack(workbook);
		assertEquals(3, wb2.getNumberOfSheets());
		assertNotNull(wb2.getSheetAt(0));
		assertNotNull(wb2.getSheetAt(1));
		assertNotNull(wb2.getSheetAt(2));
		
		assertEquals("dd/mm/yyyy", wb2.getSheetAt(0).getRow(1).getCell(0).getRichStringCellValue().getString());
		assertEquals("yyyy/mm/dd", wb2.getSheetAt(0).getRow(2).getCell(0).getRichStringCellValue().getString());
		assertEquals("yyyy-mm-dd", wb2.getSheetAt(0).getRow(3).getCell(0).getRichStringCellValue().getString());
		assertEquals("yy/mm/dd", wb2.getSheetAt(0).getRow(4).getCell(0).getRichStringCellValue().getString());
		assertNotNull(wb2.getSharedStringSource());
		assertNotNull(wb2.getStylesSource());
	}
	
	public void testStyles() {
		XSSFWorkbook workbook = XSSFTestDataSamples.openSampleWorkbook("Formatting.xlsx");
		
		StylesTable ss = workbook.getStylesSource();
		assertNotNull(ss);
		StylesTable st = ss;
		
		// Has 8 number formats
		assertEquals(8, st._getNumberFormatSize());
		// Has 2 fonts
		assertEquals(2, st.getFonts().size());
		// Has 2 fills
		assertEquals(2, st.getFills().size());
		// Has 1 border
		assertEquals(1, st.getBorders().size());
		
		// Add two more styles
		assertEquals(StylesTable.FIRST_CUSTOM_STYLE_ID + 8, 
				st.putNumberFormat("testFORMAT"));
		assertEquals(StylesTable.FIRST_CUSTOM_STYLE_ID + 8, 
				st.putNumberFormat("testFORMAT"));
		assertEquals(StylesTable.FIRST_CUSTOM_STYLE_ID + 9, 
				st.putNumberFormat("testFORMAT2"));
		assertEquals(10, st._getNumberFormatSize());
		
		
		// Save, load back in again, and check
		workbook = XSSFTestDataSamples.writeOutAndReadBack(workbook);
		
		ss = workbook.getStylesSource();
		assertNotNull(ss);

		assertEquals(10, st._getNumberFormatSize());
		assertEquals(2, st.getFonts().size());
		assertEquals(2, st.getFills().size());
		assertEquals(1, st.getBorders().size());
	}
	
	public void testNamedRanges() {
		// First up, a new file
		XSSFWorkbook workbook = new XSSFWorkbook();
		assertEquals(0, workbook.getNumberOfNames());
		
		Name nameA = workbook.createName();
		nameA.setRefersToFormula("A2");
		nameA.setNameName("ForA2");
		
		XSSFName nameB = workbook.createName();
		nameB.setRefersToFormula("B3");
		nameB.setNameName("ForB3");
		nameB.setComment("B3 Comment");
		
		// Save and re-load
		workbook = XSSFTestDataSamples.writeOutAndReadBack(workbook);
		
		assertEquals(2, workbook.getNumberOfNames());
		assertEquals("A2", workbook.getNameAt(0).getRefersToFormula());
		assertEquals("ForA2", workbook.getNameAt(0).getNameName());
		assertNull(workbook.getNameAt(0).getComment());
		
		assertEquals("B3", workbook.getNameAt(1).getRefersToFormula());
		assertEquals("ForB3", workbook.getNameAt(1).getNameName());
		assertEquals("B3 Comment", workbook.getNameAt(1).getComment());
		
		assertEquals("ForA2", workbook.getNameAt(0).getNameName());
		assertEquals(1, workbook.getNameIndex("ForB3"));
		assertEquals(-1, workbook.getNameIndex("ForB3!!"));
		
		
		// Now, an existing file with named ranges
		workbook = XSSFTestDataSamples.openSampleWorkbook("WithVariousData.xlsx");

		assertEquals(2, workbook.getNumberOfNames());
		assertEquals("Sheet1!$A$2:$A$7", workbook.getNameAt(0).getRefersToFormula());
		assertEquals("AllANumbers", workbook.getNameAt(0).getNameName());
		assertEquals("All the numbers in A", workbook.getNameAt(0).getComment());
		
		assertEquals("Sheet1!$B$2:$B$7", workbook.getNameAt(1).getRefersToFormula());
		assertEquals("AllBStrings", workbook.getNameAt(1).getNameName());
		assertEquals("All the strings in B", workbook.getNameAt(1).getComment());
		
		// Tweak, save, and re-check
		workbook.getNameAt(1).setNameName("BStringsFun");
		
		workbook = XSSFTestDataSamples.writeOutAndReadBack(workbook);
		
		assertEquals(2, workbook.getNumberOfNames());
		assertEquals("Sheet1!$A$2:$A$7", workbook.getNameAt(0).getRefersToFormula());
		assertEquals("AllANumbers", workbook.getNameAt(0).getNameName());
		assertEquals("All the numbers in A", workbook.getNameAt(0).getComment());
		
		assertEquals("Sheet1!$B$2:$B$7", workbook.getNameAt(1).getRefersToFormula());
		assertEquals("BStringsFun", workbook.getNameAt(1).getNameName());
		assertEquals("All the strings in B", workbook.getNameAt(1).getComment());
	}
	
	public void testDuplicateNames() {

		XSSFWorkbook wb = new XSSFWorkbook();
		wb.createSheet("Sheet1");
		wb.createSheet();
		wb.createSheet("name1");
		try {
		wb.createSheet("name1");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("The workbook already contains a sheet of this name", e.getMessage());
		}

		wb.createSheet();

		try {
			wb.setSheetName(3, "name1");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("The workbook already contains a sheet of this name", e.getMessage());
		}

		try {
			wb.setSheetName(3, "Sheet1");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("The workbook already contains a sheet of this name", e.getMessage());
		}

		wb.setSheetName(3, "name2");
		wb.setSheetName(3, "Sheet3");
	}
}
