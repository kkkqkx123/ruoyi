package com.ruoyi.workorder.service;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.system.domain.SysExportTask;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.IExportTaskService;
import com.ruoyi.system.service.ISysNoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;

/**
 * 异步导出服务
 */
@Service
public class ExportTaskService {

    private static final Logger log = LoggerFactory.getLogger(ExportTaskService.class);

    @Autowired
    private IExportTaskService exportTaskService;

    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 提交异步导出任务
     *
     * @param taskName       任务名称
     * @param module         所属模块
     * @param queryParamsJson 查询参数（JSON）
     * @param exportLogic    实际导出逻辑（接收文件路径参数，执行导出到该文件）
     * @return 任务ID
     */
    public Long submitExportTask(String taskName, String module,
                                  String queryParamsJson, ExportLogic exportLogic) {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        // 1. 创建任务记录
        SysExportTask task = new SysExportTask();
        task.setTaskName(taskName);
        task.setModule(module);
        task.setQueryParams(queryParamsJson);
        task.setStatus("0");
        task.setCreateBy(loginUser.getUsername());
        exportTaskService.insertExportTask(task);

        Long taskId = task.getTaskId();

        // 2. 异步执行导出
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                try {
                    // 更新状态为处理中
                    SysExportTask updateTask = new SysExportTask();
                    updateTask.setTaskId(taskId);
                    updateTask.setStatus("1");
                    exportTaskService.updateExportTask(updateTask);

                    // 生成导出文件路径
                    String fileName = module + "_" + DateUtil.format(new Date(), "yyyyMMddHHmmss")
                            + "_" + taskId + ".xlsx";
                    String filePath = RuoYiConfig.getDownloadPath() + fileName;

                    // 执行导出逻辑
                    exportLogic.export(filePath);

                    // 更新任务为已完成
                    File file = new File(filePath);
                    SysExportTask completeTask = new SysExportTask();
                    completeTask.setTaskId(taskId);
                    completeTask.setStatus("2");
                    completeTask.setFilePath(filePath);
                    completeTask.setFileSize(file.length());
                    exportTaskService.updateExportTask(completeTask);

                    // 发送站内通知
                    pushDownloadNotice(taskId, taskName, fileName);
                } catch (Exception e) {
                    log.error("异步导出失败 taskId={}", taskId, e);
                    SysExportTask failTask = new SysExportTask();
                    failTask.setTaskId(taskId);
                    failTask.setStatus("3");
                    failTask.setErrorMsg(e.getMessage());
                    exportTaskService.updateExportTask(failTask);
                }
            }
        });

        return taskId;
    }

    /**
     * 推送导出完成通知
     */
    private void pushDownloadNotice(Long taskId, String taskName, String fileName) {
        try {
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle("导出任务完成：" + taskName);
            notice.setNoticeType("1");
            notice.setNoticeContent("您的导出任务「" + taskName + "」已完成，"
                    + "请点击下载链接获取文件：/common/download?fileName=" + fileName + "&delete=false");
            notice.setStatus("0");
            notice.setCreateBy("system");
            noticeService.insertNotice(notice);
        } catch (Exception e) {
            log.warn("推送导出通知失败 taskId={}", taskId, e);
        }
    }

    /**
     * 导出逻辑接口
     */
    @FunctionalInterface
    public interface ExportLogic {
        void export(String filePath) throws Exception;
    }
}