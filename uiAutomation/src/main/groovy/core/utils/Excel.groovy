package core.utils

import com.google.common.io.Files
import groovy.util.logging.Slf4j
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook


/**
 * Created by Simion Ababi on 1/19/2019
 */
@Slf4j
class Excel {
    private Workbook workbook = new XSSFWorkbook()

    /**
     * read existing .xlsx workbook or create new file(if no parameter passed)
     * @param fileName - optional parameter: file name including path
     */
    Excel(String fileName = null) {
        if (fileName) {
            readFile(fileName)
        }
    }

    /**
     * If no parameters passed, creates sheet with default name
     * @param sheetName - optional parameter: if sheet with the name exists, adds "_1" to the end of the name
     */
    void createSheet(String sheetName = null) {
        if (!sheetName) {
            log.debug "creating sheet with default name"
            String name = workbook.createSheet().getSheetName()
            setActiveSheet(name)
        } else {
            if (workbook.sheetIterator().find { it.getSheetName() == sheetName }) {
                sheetName = "${sheetName}_1"
                createSheet(sheetName)
            } else {
                log.debug "creating sheet with name='$sheetName'"
                workbook.createSheet(sheetName)
                setActiveSheet(sheetName)
            }
        }
    }

    /**
     * sets sheet to be current/active
     * @param sheet - Integer (index of sheet) or String (name of the sheet)
     */
    void setActiveSheet(sheet) {
        Integer sheetIndex = null
        if (sheet instanceof Integer)
            sheetIndex = (Integer) sheet
        else if (sheet instanceof String)
            sheetIndex = workbook.getSheetIndex(sheet as String)
        else
            throw new IllegalArgumentException("Only parameter type of Integer or String is allowed")

        workbook.setActiveSheet(sheetIndex)
        log.debug "Switched to sheet ${workbook.getSheetName(sheetIndex)}"
    }

    int getFirstRowNum() {
        Sheet sheet = workbook.getSheetAt(workbook.activeSheetIndex)
        sheet.getFirstRowNum()
    }

    int getLastRowNum() {
        Sheet sheet = workbook.getSheetAt(workbook.activeSheetIndex)
        sheet.getLastRowNum()
    }

    /**
     * returns cells contents of the row as List of Strings
     * @param rowNum - Integer (the number of the row)
     * @param firstCellIndex - Integer (the first cell index)
     * @param lastCellIndex - Integer (the last cell index)
     * @return list of strings of cells contents
     */
    List<String> getAllTextsInRow(Integer rowNum, Integer firstCellIndex = 0, Integer lastCellIndex = 0) {
        ArrayList<String> result = []
        DataFormatter formatter = new DataFormatter()

        if (firstCellIndex > lastCellIndex) {
            log.debug("Method getAllTextsInRow(): The first colum number $firstCellIndex is bigger then the last column number $lastCellIndex")
            return result
        }

        Sheet sheet = workbook.getSheetAt(workbook.activeSheetIndex)
        Row row = sheet.getRow(rowNum)
        if (row == null) {
            // This whole row is empty
            log.debug("Method getAllTextsInRow(): The row $rowNum is empty")
        } else {
            def cellRange = firstCellIndex..lastCellIndex
            cellRange.each { cn ->
                Cell cell = row.getCell(cn)
                result << formatter.formatCellValue(cell)
            }
        }
        return result
    }

    private void addCustomRow(List<String> rowInfo, Integer rowNum = null, Integer firstCellIndex = 0, boolean isHeader = false) {
        Sheet sheet = workbook.getSheetAt(workbook.activeSheetIndex)

        if (!rowNum) {
            int lastRowNum = sheet.lastRowNum
            if (!sheet.getRow(lastRowNum))
                rowNum = 0
            else
                rowNum = lastRowNum + 1
        }
        log.debug "adding row:$rowInfo at index $rowNum starting with cell $firstCellIndex"

        Row currentRow = sheet.createRow(rowNum)
        CellStyle headerStyle = getHeaderStyle()
        CellStyle regularStyle = getRegularStyle()
        rowInfo.eachWithIndex { text, cellIndex ->
            Cell cell = currentRow.createCell(firstCellIndex + cellIndex)
            cell.setCellValue(text)
            if (isHeader)
                cell.setCellStyle(headerStyle)
            else {
                if (rowNum % 2 == 0)
                    cell.setCellStyle(regularStyle)
            }
        }
    }

    private CellStyle getHeaderStyle() {
        Font font = workbook.createFont()
        font.setBold(true)
        CellStyle cellStyle = workbook.createCellStyle()
        cellStyle.setAlignment(HorizontalAlignment.CENTER)
        cellStyle.setFont(font)
        return cellStyle
    }

    private CellStyle getRegularStyle() {
        CellStyle cellStyle = workbook.createCellStyle()
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index)
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        return cellStyle
    }

    void addHeaders(List<String> rowInfo, Integer rowNum = null, Integer firstCellIndex = 0) {
        addCustomRow(rowInfo, rowNum, firstCellIndex, true)
    }

    void addRow(List<String> rowInfo, Integer rowNum = null, Integer firstCellIndex = 0) {
        addCustomRow(rowInfo, rowNum, firstCellIndex, false)
    }

    void readFile(String fileName) {
        File file = new File(fileName)
        if (file.exists()) {
            log.debug "opening file $fileName"
            workbook = WorkbookFactory.create(file.newInputStream())
        } else {
            log.debug "file $fileName not found, creating new workbook"
        }
    }

    /**
     * creates new .xlsx file (and directories if don't exist) and writes current(in memory) version of the workbook into it.
     * @param fileName - file name including path
     * @param overwrite - if true overwrites file if exists by given path, otherwise creates new one by adding current time in ms to the end of the file name
     */
    void writeToFile(String fileName, boolean overwrite = true) {
        log.debug "writing current workbook into file: $fileName"
        File file = new File(fileName)
        if (file.exists() && !overwrite) {
            log.debug "file $fileName exists, adding current time to file name"
            String newFileName = "${file.parent}/" + Files.getNameWithoutExtension(fileName) + "_${new Date().time}." + Files.getFileExtension(fileName)
            file = new File(newFileName)
        }
        Files.createParentDirs(file)
        workbook.write(file.newOutputStream())
    }
}
