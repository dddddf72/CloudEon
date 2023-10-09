/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dromara.cloudeon.processor;

import ch.qos.logback.classic.ClassicConstants;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.CommandRepository;
import org.dromara.cloudeon.dao.CommandTaskRepository;
import org.dromara.cloudeon.entity.CommandEntity;
import org.dromara.cloudeon.entity.CommandTaskEntity;
import org.dromara.cloudeon.enums.CommandState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.cloudeon.utils.RemoteSshTaskErrorLineHandler;
import org.dromara.cloudeon.utils.RemoteSshTaskLineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseCloudeonTask implements Runnable {

    protected Logger log = LoggerFactory.getLogger(getClass().getName());

    public static final String TASKID = "taskId";
    public static final String TASK_LOG_HOME = "TASK_LOG_HOME";

    protected CommandTaskRepository commandTaskRepository;
    protected CommandRepository commandRepository;
    protected TaskParam taskParam;

    protected RemoteSshTaskLineHandler remoteSshTaskLineHandler;
    protected RemoteSshTaskErrorLineHandler remoteSshTaskErrorLineHandler;



    private void init() {
        remoteSshTaskLineHandler = new RemoteSshTaskLineHandler(log);
        remoteSshTaskErrorLineHandler = new RemoteSshTaskErrorLineHandler(log);
        // 填充数据库操作类
        commandTaskRepository = SpringUtil.getBean(CommandTaskRepository.class);
        commandRepository = SpringUtil.getBean(CommandRepository.class);
        // 日志标识
        MDC.put(TASKID, (taskParam.getCommandId() + "-" + taskParam.getCommandTaskId()));
        // 记录任务开始时间
        CommandTaskEntity commandTaskEntity = commandTaskRepository.findById(taskParam.getCommandTaskId()).get();
        log.info("command task：" + taskParam.getCommandTaskId() + " 开始, 记录到数据库");
        commandTaskEntity.setStartTime(new Date());
        commandTaskEntity.setCommandState(CommandState.RUNNING);
        commandTaskEntity.setProgress(10);
        commandTaskRepository.saveAndFlush(commandTaskEntity);

    }

    @Override
    public void run() {
        init();

        try {
            internalExecute();
        } catch (Exception e) {
            doWhenError(e);
        }
        after();

    }

    private void after() {
        log.info("command task：" + taskParam.getCommandTaskId() + " 结束, 记录到数据库");
        CommandTaskEntity commandTaskEntity = commandTaskRepository.findById(taskParam.getCommandTaskId()).get();
        commandTaskEntity.setEndTime(new Date());
        commandTaskEntity.setCommandState(CommandState.SUCCESS);
        commandTaskEntity.setProgress(100);
        commandTaskRepository.saveAndFlush(commandTaskEntity);

        //主动让SiftingAppender结束文件.
        log.info(ClassicConstants.FINALIZE_SESSION_MARKER, "close sifting Appender of `{}`", MDC.get(TASKID));

        //清理MDC.
        MDC.clear();

        // 更新command进度
        CommandEntity updateCommandEntity = commandRepository.findById(taskParam.getCommandId()).get();
        // 计算进度
        Integer successTaskCnt = commandTaskRepository.countByCommandStateAndCommandId(CommandState.SUCCESS, taskParam.getCommandId());
        Integer totalTaskCnt = commandTaskRepository.countByCommandId(taskParam.getCommandId());
        Double progress = Math.floor(successTaskCnt.doubleValue() / totalTaskCnt.doubleValue() * 100);
        updateCommandEntity.setCurrentProgress(progress.intValue());
        commandRepository.saveAndFlush(updateCommandEntity);

    }

    private void doWhenError(Exception e) {
        log.error("CommandTask:"+taskParam.getCommandTaskId() + "发生异常，处理异常。。。" + e.getMessage(),e);
        CommandTaskEntity commandTaskEntity = commandTaskRepository.findById(taskParam.getCommandTaskId()).get();
        commandTaskEntity.setEndTime(new Date());
        commandTaskEntity.setCommandState(CommandState.ERROR);
        commandTaskRepository.saveAndFlush(commandTaskEntity);

        // 更新command状态
        CommandEntity updateCommandEntity = commandRepository.findById(taskParam.getCommandId()).get();
        updateCommandEntity.setCommandState(CommandState.ERROR);
        commandRepository.saveAndFlush(updateCommandEntity);
        throw new RuntimeException();
    }

    abstract public void internalExecute();


    private void doSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}