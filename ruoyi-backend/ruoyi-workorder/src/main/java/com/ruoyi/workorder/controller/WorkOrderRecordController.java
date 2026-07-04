package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.service.IWorkOrderRecordService;
import com.ruoyi.workorder.service.IWorkOrderService;
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

import java.util.List;

/**
 * 工单维修记录Controller
 */
@RestController
@RequestMapping("/workorder/record")
public class WorkOrderRecordController extends BaseController {

    @Autowired
    private IWorkOrderRecordService workOrderRecordService;

    @Autowired
    private IWorkOrderService workOrderService;

    @Autowired
    private TokenService tokenService;

    /**
     * 查询维修记录列表
     */
    @PreAuthorize("@ss.hasPermi('workorder:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkOrderRecord workOrderRecord) {
        startPage();
        List<WorkOrderRecord> list = workOrderRecordService.selectWorkOrderRecordList(workOrderRecord);
        return getDataTable(list);
    }

    /**
     * 获取维修记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('workorder:record:query')")
    @GetMapping("/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId) {
        return success(workOrderRecordService.selectWorkOrderRecordById(recordId));
    }

    /**
     * 新增维修记录（同时完成工单）
     */
    @PreAuthorize("@ss.hasPermi('workorder:record:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WorkOrderRecord workOrderRecord) {
        LoginUser loginUser = getLoginUser();
        workOrderRecord.setRepairBy(loginUser.getUsername());
        workOrderRecord.setRepairTime(new java.util.Date());
        workOrderService.completeWorkOrder(workOrderRecord);
        return success();
    }

    /**
     * 修改维修记录
     */
    @PreAuthorize("@ss.hasPermi('workorder:record:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WorkOrderRecord workOrderRecord) {
        return toAjax(workOrderRecordService.updateWorkOrderRecord(workOrderRecord));
    }

    /**
     * 删除维修记录
     */
    @PreAuthorize("@ss.hasPermi('workorder:record:remove')")
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) {
        return toAjax(workOrderRecordService.deleteWorkOrderRecordByIds(recordIds));
    }
}