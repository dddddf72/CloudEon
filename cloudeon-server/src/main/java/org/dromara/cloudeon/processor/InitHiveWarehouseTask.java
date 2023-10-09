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
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.dromara.cloudeon.dao.*;
import org.dromara.cloudeon.dto.VolumeMountDTO;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.dromara.cloudeon.entity.StackServiceEntity;
import org.dromara.cloudeon.service.KubeService;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.JschUtils;
import org.dromara.cloudeon.utils.K8sUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.dromara.cloudeon.utils.Constant.DEFAULT_JSCH_TIMEOUT;

@NoArgsConstructor
public class InitHiveWarehouseTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ServiceRoleInstanceRepository roleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        KubeService kubeService = SpringUtil.getBean(KubeService.class);
        ClusterInfoRepository clusterInfoRepository = SpringUtil.getBean(ClusterInfoRepository.class);

        TaskParam taskParam = getTaskParam();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
        // 获取集群的namespace
        String namespace = clusterInfoRepository.findById(serviceInstanceEntity.getClusterId()).get().getNamespace();
        if (StringUtils.isBlank(namespace)) {
            namespace = "default";
        }

        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(serviceInstanceEntity.getStackServiceId()).get();
        String serviceName = serviceInstanceEntity.getServiceName();
        // 选择metastore所在节点执行
        List<ServiceRoleInstanceEntity> roleInstanceEntities = roleInstanceRepository.findByServiceInstanceIdAndServiceRoleName(serviceInstanceId, "HIVE_METASTORE");
        ServiceRoleInstanceEntity firstNamenode = roleInstanceEntities.get(0);
        Integer nodeId = firstNamenode.getNodeId();
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findById(nodeId).get();

        // 启动K8S job
        String jobCmd = String.format("/opt/edp/%s/conf/init-warehouse-dir.sh",serviceName);
        String volumePath = String.format("/opt/edp/%s/conf", serviceName);
        KubernetesClient kubeClient = kubeService.getKubeClient(serviceInstanceEntity.getClusterId());
        VolumeMountDTO[] volumeMounts = {new VolumeMountDTO("config-volume", volumePath, volumePath)};
        K8sUtil.runJob(namespace,"init-hive-warehouse", kubeClient, volumeMounts, stackServiceEntity.getDockerImage(), jobCmd, log, nodeEntity.getHostname());



    }
}
