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

import cn.hutool.extra.spring.SpringUtil;
import com.jcraft.jsch.Session;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.StackServiceRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.StackServiceEntity;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.JschUtils;
import org.dromara.cloudeon.utils.SshUtils;
import lombok.NoArgsConstructor;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.dromara.cloudeon.utils.Constant.DEFAULT_JSCH_TIMEOUT;

@NoArgsConstructor
public class PullImageTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        // 查询安装服务的镜像
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(taskParam.getStackServiceId()).get();
        String dockerImage = stackServiceEntity.getDockerImage();
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findByHostname(taskParam.getHostName());
        log.info("节点：" + taskParam.getHostName() + " 上拉取镜像：" + dockerImage);
        String command = "";
        String runtimeContainer = nodeEntity.getRuntimeContainer();
        // 兼容containerd
        if (runtimeContainer.startsWith("docker")) {
            command=  "docker pull " + dockerImage;
        } else if (runtimeContainer.startsWith("containerd")) {
            command=  "ctr image pull " + dockerImage;
        }
        // ssh执行拉镜像
        Session clientSession = sshPoolService.openSession(nodeEntity);
        try {
            log.info("节点：" + taskParam.getHostName() + " 上执行命令：" + command);
            JschUtils.execCallbackLine(clientSession, Charset.defaultCharset(), DEFAULT_JSCH_TIMEOUT,command ,null,remoteSshTaskLineHandler,remoteSshTaskErrorLineHandler );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("成功在节点：" + taskParam.getHostName() + " 上拉取镜像：" + dockerImage);




    }
}
