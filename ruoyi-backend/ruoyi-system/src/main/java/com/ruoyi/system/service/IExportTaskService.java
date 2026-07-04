package com.ruoyi.system.service;

import com.ruoyi.system.domain.SysExportTask;
import java.util.List;

/**
 * 导出任务记录 服务层
 */
public interface IExportTaskService {

    /**
     * 查询导出任务记录
     */
    public SysExportTask selectExportTaskById(Long taskId);

    /**
     * 查询导出任务记录列表
     */
    public List<SysExportTask> selectExportTaskList(SysExportTask task);

    /**
     * 新增导出任务记录
     */
    public int insertExportTask(SysExportTask task);

    /**
     * 修改导出任务记录
     */
    public int updateExportTask(SysExportTask task);

    /**
     * 删除导出任务记录
     */
    public int deleteExportTaskById(Long taskId);
}