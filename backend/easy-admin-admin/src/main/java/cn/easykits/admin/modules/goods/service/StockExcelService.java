package cn.easykits.admin.modules.goods.service;

import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.exception.BusinessException;
import cn.easykits.admin.modules.goods.vo.BatchStockVO;
import cn.easykits.admin.modules.goods.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存 Excel：批量设置库存模板（预填全部商品）、文件解析。
 * <p>与订单批量发货同一套交互：下载模板 → 改「目标库存」列 → 上传导入，逐条返回失败原因。</p>
 */
@Service
@RequiredArgsConstructor
public class StockExcelService {

    /** 模板预填商品上限 */
    private static final int TEMPLATE_MAX = 5000;

    private static final String[] HEADERS = {"商品编码", "商品名称", "目标库存"};

    private final StockService stockService;

    /* ---------------- 模板 ---------------- */

    /**
     * 批量设置库存模板：预填全部商品的编码、名称与当前库存，
     * 运营只需修改「目标库存」列；不需要调整的行整行删除或清空编码即可。
     */
    public byte[] stockTemplate() {
        PageResult<StockVO> page = stockService.pageQuery(1, TEMPLATE_MAX, null, null);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("批量设置库存");
            CellStyle headStyle = headStyle(workbook);
            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headStyle);
            }
            sheet.setColumnWidth(0, 24);
            sheet.setColumnWidth(1, 34);
            sheet.setColumnWidth(2, 14);

            int rowIdx = 1;
            for (StockVO vo : page.records()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(vo.goodsCode() == null ? "" : vo.goodsCode());
                row.createCell(1).setCellValue(vo.name() == null ? "" : vo.name());
                row.createCell(2).setCellValue(vo.stock() == null ? 0 : vo.stock());
            }

            // 说明行：编码列留空，解析时按空编码跳过
            CellStyle tipStyle = tipStyle(workbook);
            Row tip = sheet.createRow(rowIdx + 1);
            Cell tipCell = tip.createCell(1);
            tipCell.setCellValue("说明：只需修改「目标库存」列；与当前库存一致的商品导入后无变化；"
                    + "不需要调整的行请整行删除或清空编码；失败原因在导入结果中逐条返回。");
            tipCell.setCellStyle(tipStyle);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new BusinessException("生成库存模板失败");
        }
    }

    /* ---------------- 解析上传文件 ---------------- */

    /**
     * 解析上传的库存文件：三列固定顺序为 商品编码 / 商品名称 / 目标库存，从第 2 行开始读，
     * 跳过空行与「说明」行（编码列为空即忽略），名称列仅作展示不参与处理。
     */
    public BatchStockVO parseAndBatchSet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传库存设置文件");
        }
        List<StockService.BatchStockRow> rows = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = readString(row.getCell(0));
                if (!StringUtils.hasText(code)) {
                    continue;
                }
                rows.add(new StockService.BatchStockRow(code, readString(row.getCell(2))));
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("文件解析失败，请使用「下载模板」生成的 xlsx 文件填写");
        }
        if (rows.isEmpty()) {
            throw new BusinessException("文件中没有可设置库存的数据行");
        }
        return stockService.batchSetStock(rows);
    }

    /* ---------------- private ---------------- */

    private String readString(Cell cell) {
        if (cell == null) {
            return "";
        }
        // 库存可能被识别成数字，编码与数量一律按字符串读
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (RuntimeException e) {
                    yield "";
                }
            }
            default -> "";
        };
    }

    private CellStyle headStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle tipStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font tipFont = workbook.createFont();
        tipFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        tipFont.setItalic(true);
        style.setFont(tipFont);
        return style;
    }

    private byte[] toBytes(XSSFWorkbook workbook) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("生成 Excel 文件失败");
        }
    }
}
