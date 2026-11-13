package cn.easykits.admin.modules.order.service;

import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.exception.BusinessException;
import cn.easykits.admin.modules.order.vo.BatchDeliverVO;
import cn.easykits.admin.modules.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单 Excel：导出、批量发货模板、批量发货文件解析。
 * <p>导出复用 {@link OrderService#pageQuery} 的过滤与 VO 组装，上限 {@link #EXPORT_MAX} 行，
 * 防止全量导出拖垮内存。</p>
 */
@Service
@RequiredArgsConstructor
public class OrderExcelService {

    /** 单次导出上限 */
    private static final int EXPORT_MAX = 5000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] EXPORT_HEADERS = {
            "订单号", "订单状态", "售后状态", "买家", "收货人", "联系电话", "收货地址",
            "商品明细", "订单总额", "运费", "优惠合计", "实付金额", "支付方式",
            "下单时间", "付款时间", "发货时间", "快递公司", "快递单号", "买家备注"};

    private static final String[] DELIVER_HEADERS = {"订单号", "快递公司", "快递单号"};

    private final OrderService orderService;

    /* ---------------- 导出 ---------------- */

    public byte[] exportOrders(String keyword, Integer status, Integer refundStatus,
                               LocalDate startDate, LocalDate endDate) {
        PageResult<OrderVO> page = orderService.pageQuery(1, EXPORT_MAX, keyword, status,
                refundStatus, startDate, endDate);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单列表");
            CellStyle headStyle = headStyle(workbook);
            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, i == 6 || i == 7 ? 30 : 18);
            }
            CellStyle moneyStyle = moneyStyle(workbook);
            int rowIdx = 1;
            for (OrderVO vo : page.records()) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(vo.orderNo());
                row.createCell(c++).setCellValue(statusText(vo.status()));
                row.createCell(c++).setCellValue(refundText(vo.refundStatus()));
                row.createCell(c++).setCellValue(nullSafe(vo.userNickname()));
                row.createCell(c++).setCellValue(nullSafe(vo.receiverName()));
                row.createCell(c++).setCellValue(nullSafe(vo.receiverPhone()));
                row.createCell(c++).setCellValue(nullSafe(vo.receiverAddress()));
                row.createCell(c++).setCellValue(goodsSummary(vo));
                cellMoney(row, c++, vo.totalAmount(), moneyStyle);
                cellMoney(row, c++, vo.freightAmount(), moneyStyle);
                cellMoney(row, c++, discountTotal(vo), moneyStyle);
                cellMoney(row, c++, vo.payAmount(), moneyStyle);
                row.createCell(c++).setCellValue(payTypeText(vo.payType()));
                row.createCell(c++).setCellValue(format(vo.createdAt()));
                row.createCell(c++).setCellValue(format(vo.payTime()));
                row.createCell(c++).setCellValue(format(vo.deliveryTime()));
                row.createCell(c++).setCellValue(nullSafe(vo.deliveryCompany()));
                row.createCell(c++).setCellValue(nullSafe(vo.deliveryNo()));
                row.createCell(c++).setCellValue(nullSafe(vo.remark()));
            }
            return toBytes(workbook);
        } catch (IOException e) {
            throw new BusinessException("生成导出文件失败");
        }
    }

    /* ---------------- 批量发货模板 ---------------- */

    public byte[] deliverTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("批量发货");
            CellStyle headStyle = headStyle(workbook);
            Row head = sheet.createRow(0);
            for (int i = 0; i < DELIVER_HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(DELIVER_HEADERS[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 28);
            }
            // 示例行：下单时把示例数据删掉即可
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("EM20261015000008");
            example.createCell(1).setCellValue("顺丰速运");
            example.createCell(2).setCellValue("SF1234567890");
            CellStyle tipStyle = workbook.createCellStyle();
            Font tipFont = workbook.createFont();
            tipFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            tipFont.setItalic(true);
            tipStyle.setFont(tipFont);
            Row tip = sheet.createRow(3);
            Cell tipCell = tip.createCell(0);
            tipCell.setCellValue("说明：仅「待发货」状态的订单会发货；重复行只处理首次出现；失败原因在导入结果中逐条返回。");
            tipCell.setCellStyle(tipStyle);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new BusinessException("生成发货模板失败");
        }
    }

    /* ---------------- 解析批量发货文件 ---------------- */

    /**
     * 解析上传的发货文件：三列固定顺序为 订单号 / 快递公司 / 快递单号，从第 2 行开始读，
     * 跳过空行与「说明」等非数据行（订单号列解析不到订单号格式即忽略）。
     */
    public BatchDeliverVO parseAndBatchDeliver(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传发货文件");
        }
        List<OrderService.BatchDeliverRow> rows = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String orderNo = readString(row.getCell(0));
                // 说明行等非数据行直接跳过（订单号至少 8 位）
                if (!StringUtils.hasText(orderNo) || orderNo.length() < 8) {
                    continue;
                }
                rows.add(new OrderService.BatchDeliverRow(
                        orderNo,
                        readString(row.getCell(1)),
                        readString(row.getCell(2))));
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("文件解析失败，请使用「下载模板」生成的 xlsx 文件填写");
        }
        if (rows.isEmpty()) {
            throw new BusinessException("文件中没有可发货的数据行");
        }
        return orderService.batchDeliver(rows);
    }

    /* ---------------- private ---------------- */

    private String readString(Cell cell) {
        if (cell == null) {
            return "";
        }
        // 单号可能被识别成数字，一律按字符串读
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

    private CellStyle moneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00"));
        return style;
    }

    private void cellMoney(Row row, int col, java.math.BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private java.math.BigDecimal discountTotal(OrderVO vo) {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (java.math.BigDecimal part : new java.math.BigDecimal[]{
                vo.couponAmount(), vo.memberDiscount(), vo.reductionAmount()}) {
            if (part != null) {
                total = total.add(part);
            }
        }
        return total;
    }

    private String goodsSummary(OrderVO vo) {
        List<String> parts = vo.items().stream()
                .map(item -> item.getGoodsName() + "x" + item.getQuantity())
                .toList();
        String summary = String.join("; ", parts);
        return summary.length() > 200 ? summary.substring(0, 200) : summary;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String refundText(Integer refundStatus) {
        if (refundStatus == null) {
            return "";
        }
        return switch (refundStatus) {
            case 0 -> "无售后";
            case 1 -> "退款中";
            case 2 -> "已退款";
            case 3 -> "售后被拒";
            default -> "未知";
        };
    }

    private String payTypeText(Integer payType) {
        if (payType == null) {
            return "未知（存量订单）";
        }
        return switch (payType) {
            case 0 -> "模拟支付";
            case 1 -> "微信支付";
            case 2 -> "支付宝";
            default -> "未知";
        };
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : TIME_FORMAT.format(time);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
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
