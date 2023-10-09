<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<configuration scan="true" scanPeriod="120 seconds">
    <property name="log.base" value="/opt/edp/${service.serviceName}/log"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                [%level] %date{yyyy-MM-dd HH:mm:ss.SSS Z} %logger{96}:[%line] - %msg%n
            </pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="APILOGFILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${r"${log.base}"}/dolphinscheduler-api.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${r"${log.base}"}/dolphinscheduler-api.%d{yyyy-MM-dd_HH}.%i.log</fileNamePattern>
            <maxHistory>168</maxHistory>
            <maxFileSize>64MB</maxFileSize>
            <totalSizeCap>50GB</totalSizeCap>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
        <encoder>
            <pattern>
                [%level] %date{yyyy-MM-dd HH:mm:ss.SSS Z} %logger{96}:[%line] - %msg%n
            </pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="org.apache.zookeeper" level="WARN"/>
    <logger name="org.apache.hbase" level="WARN"/>
    <logger name="org.apache.hadoop" level="WARN"/>

    <root level="INFO">
        <if condition="true">
            <then>
                <appender-ref ref="STDOUT"/>
            </then>
        </if>
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="APILOGFILE"/>
    </root>
</configuration>
