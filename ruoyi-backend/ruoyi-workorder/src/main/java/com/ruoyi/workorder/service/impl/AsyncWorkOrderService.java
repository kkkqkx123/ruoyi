package com.ruoyi.workorder.service.impl;

import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;
import com.ruoyi.workorder.domain.WorkOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 工单异步任务服务
 * <p>
 * 将耗时操作（通知推送、统计刷新等）异步化，
 * 避免阻塞主流程。单独提取为一个 Service 以保证 {@link Async} 通过代理生效。
 */
@Service
public class AsyncWorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(AsyncWorkOrderService.class);

    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 异步推送紧急工单通知
     */
    @Async("threadPoolTaskExecutor")
    public void pushUrgentNotice(WorkOrder workOrder) {
        try {
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle("紧急工单：" + workOrder.getOrderNo());
            notice.setNoticeType("1");
            notice.setNoticeContent("有新的紧急工单 " + workOrder.getOrderNo()
                    + " 需要处理，故障描述：" + workOrder.getFaultDesc()
                    + "，请及时派单。");
            notice.setStatus("0");
            notice.setCreateBy("system");
            noticeService.insertNotice(notice);
        } catch (Exception e) {
            log.error("推送紧急工单通知失败，orderId={}, orderNo={}",
                    workOrder.getOrderId(), workOrder.getOrderNo(), e);
        }
    }
}