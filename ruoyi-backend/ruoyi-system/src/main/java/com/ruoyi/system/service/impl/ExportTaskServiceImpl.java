package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SysExportTask;
import com.ruoyi.system.mapper.SysExportTaskMapper;
import com.ruoyi.system.service.IExportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导出任务记录 服务实现
 */
@Service
public class ExportTaskServiceImpl implements IExportTaskService {

    @Autowired
    private SysExportTaskMapper exportTaskMapper;

    @Override
    public SysExportTask selectExportTaskById(Long taskId) {
        return exportTaskMapper.selectExportTaskById(taskId);
    }

    @Override
    public List<SysExportTask> selectExportTaskList(SysExportTask task) {
        return exportTaskMapper.selectExportTaskList(task);
    }

    @Override
    public int insertExportTask(SysExportTask task) {
        return exportTaskMapper.insertExportTask(task);
    }

    @Override
    public int updateExportTask(SysExportTask task) {
        return exportTaskMapper.updateExportTask(task);
    }

    @Override
    public int deleteExportTaskById(Long taskId) {
        return exportTaskMapper.deleteExportTaskById(taskId);
    }
}