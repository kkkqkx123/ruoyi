package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.workorder.domain.FaultTopDevice;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.service.ExportTaskService;
import com.ruoyi.workorder.service.IWorkOrderService;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 工单主表Controller
 */
@RestController
@RequestMapping("/workorder/order")
public class WorkOrderController extends BaseController {

    @Autowired
    private IWorkOrderService workOrderService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ExportTaskService exportTaskService;

    /**
     * 查询工单列表
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkOrder workOrder) {
        // 限制最大分页深度，防止深分页性能退化
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (pageDomain.getPageNum() > 1000) {
            pageDomain.setPageNum(1000);
        }
        startPage();
        List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder);
        return getDataTable(list);
    }

    /**
     * 获取工单详细信息
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId) {
        return success(workOrderService.selectWorkOrderById(orderId));
    }

    /**
     * 新增工单
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WorkOrder workOrder) {
        return toAjax(workOrderService.insertWorkOrder(workOrder));
    }

    /**
     * 修改工单
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WorkOrder workOrder) {
        return toAjax(workOrderService.updateWorkOrder(workOrder));
    }

    /**
     * 删除工单
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:remove')")
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds) {
        return toAjax(workOrderService.deleteWorkOrderByIds(orderIds));
    }

    /**
     * 批量分配工单
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:assign')")
    @PutMapping("/batchAssign")
    public AjaxResult batchAssign(@RequestBody Map<String, Object> params) {
        List<Integer> orderIdList = (List<Integer>) params.get("orderIds");
        String assignTo = (String) params.get("assignTo");
        Long[] orderIds = orderIdList.stream().map(Long::valueOf).toArray(Long[]::new);
        return toAjax(workOrderService.batchAssign(orderIds, assignTo));
    }

    /**
     * 获取工单统计看板
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:stats')")
    @GetMapping("/stats")
    public AjaxResult stats() {
        WorkOrderStats stats = workOrderService.selectWorkOrderStats();
        if (stats != null) {
            List<FaultTopDevice> faultTopDevices = workOrderService.selectFaultTopDevices();
            stats.setFaultTopDevices(faultTopDevices);
        }
        return success(stats);
    }

    /**
     * 导出工单列表（流式导出，避免 OOM）
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:export')")
    @GetMapping("/export")
    public void export(HttpServletResponse response, WorkOrder workOrder) {
        ExcelUtil<WorkOrder> util = new ExcelUtil<>(WorkOrder.class);
        util.streamExportExcel(response,
                (pageNum, pageSize) -> {
                    PageHelper.startPage(pageNum, pageSize);
                    return workOrderService.selectWorkOrderList(workOrder);
                },
                "工单数据", "工单导出", 5000);
    }

    /**
     * 异步导出工单（适合超大数据量）
     * 提交后立即返回任务ID，后台异步执行导出
     * 完成后通过站内通知提醒用户下载
     */
    @PreAuthorize("@ss.hasPermi('workorder:order:export')")
    @GetMapping("/asyncExport")
    public AjaxResult asyncExport(WorkOrder workOrder) {
        String paramsJson = JSON.toJSONString(workOrder);
        Long taskId = exportTaskService.submitExportTask(
                "工单数据导出",
                "workorder",
                paramsJson,
                filePath -> doExportWorkOrder(workOrder, filePath)
        );
        return success("导出任务已提交，任务ID：" + taskId);
    }

    /**
     * 实际的工单导出逻辑（写入本地文件）
     */
    private void doExportWorkOrder(WorkOrder workOrder, String filePath) throws Exception {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {
            // 使用 SXSSFWorkbook 直接在构造函数中创建
            org.apache.poi.xssf.streaming.SXSSFWorkbook wb =
                    new org.apache.poi.xssf.streaming.SXSSFWorkbook(500);
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("工单数据");

            // 创建表头
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("工单编号");
            headerRow.createCell(1).setCellValue("设备名称");
            headerRow.createCell(2).setCellValue("故障描述");
            headerRow.createCell(3).setCellValue("工单状态");
            headerRow.createCell(4).setCellValue("报修人");
            headerRow.createCell(5).setCellValue("维修员");
            headerRow.createCell(6).setCellValue("创建时间");

            // 分批查询并写入
            int pageNum = 1;
            int pageSize = 5000;
            boolean hasMore = true;

            while (hasMore) {
                PageHelper.startPage(pageNum, pageSize);
                List<WorkOrder> batch = workOrderService.selectWorkOrderList(workOrder);
                if (batch == null || batch.isEmpty()) {
                    hasMore = false;
                } else {
                    int startRow = (pageNum - 1) * pageSize + 1;
                    for (int i = 0; i < batch.size(); i++) {
                        WorkOrder item = batch.get(i);
                        org.apache.poi.ss.usermodel.Row row = sheet.createRow(startRow + i);
                        row.createCell(0).setCellValue(item.getOrderNo());
                        row.createCell(1).setCellValue(item.getDeviceName());
                        row.createCell(2).setCellValue(item.getFaultDesc());
                        row.createCell(3).setCellValue(item.getOrderStatus());
                        row.createCell(4).setCellValue(item.getReporterBy());
                        row.createCell(5).setCellValue(item.getAssignTo());
                        row.createCell(6).setCellValue(item.getCreateTime() != null
                                ? item.getCreateTime().toString() : "");
                    }
                    // 刷出到临时文件
                    if (wb.getSheetAt(0) instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
                        ((org.apache.poi.xssf.streaming.SXSSFSheet) sheet).flushRows(batch.size());
                    }
                    pageNum++;
                    if (batch.size() < pageSize) {
                        hasMore = false;
                    }
                }
            }

            wb.write(fos);
            wb.dispose();
        }
    }
}