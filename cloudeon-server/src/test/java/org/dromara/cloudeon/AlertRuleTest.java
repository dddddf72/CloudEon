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

package org.dromara.cloudeon;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.dao.AlertQuotaRepository;
import org.dromara.cloudeon.dao.ClusterInfoRepository;
import org.dromara.cloudeon.dto.PrometheusAlertRule;
import org.dromara.cloudeon.entity.ClusterAlertQuotaEntity;
import org.dromara.cloudeon.service.AlertService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AlertRuleTest {

    @Resource
    private AlertService alertService;


    @Test
    public void dod() {

        alertService.upgradeMonitorAlertRule(1,log);
    }


}