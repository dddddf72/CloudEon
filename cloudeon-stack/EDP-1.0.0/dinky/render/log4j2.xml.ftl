<?xml version="1.0" encoding="UTF-8"?>
<!--status用于设置log4j2框架内部的日志信息输出，设置成OFF将禁止log4j2内部日志输出，毕竟这个日志对我们没有什么作用，如果设置成trace，你会看到log4j2内部各种详细输出；monitorInterval是监控间隔，例如下面的设置是指：log4j2每隔600秒自动监控该配置文件是否有变化，如果有变化，则根据文件内容新的配置生成日志-->
<configuration status="OFF" monitorInterval="600">
    <Properties>
        <property name="LOG_PATH">/opt/edp/${service.serviceName}/log</property>
        <property name="LOG_FILE">dinky</property>
    </Properties>
    <!--定义添加器-->
    <appenders>
        <!--Console是输出控制台的标签，target可以控制往控制台输出日志的颜色，例如SYSTEM_OUT就是蓝色的，SYSTEM_ERR就是红色的-->
        <Console name="Console" target="SYSTEM_OUT">
            <!--控制台只输出level及以上级别的信息，onMatch为true代表符合level标准的才输出，onMismatch为true代表不符合level标准的就不输出-->
            <ThresholdFilter level="info" onMatch="ACCEPT" onMismatch="ACCEPT"/>
            <!--这个是输出日志的格式，如果对里面的参数不理解，可以去看我的这篇文章，网址是：“https://blog.csdn.net/qq_42449963/article/details/104617356”-->
            <!--<PatternLayout pattern="[dinky] %d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %class{36} %L %M - %msg%xEx%n"/>-->
            <PatternLayout pattern="[dinky] %d{yyyy-MM-dd HH:mm:ss.SSS} %highlight{%6p} %style{%5pid}{bright,magenta} --- [%15.15t] %style{%c{20}}{bright,cyan}: %m%n"/>
        </Console>
        <!--这种存储文件的方式更加合理，可以设置多长时间把文件归档一次，也可以设置多大文件归档一次，如果都把所有的日志存在一个文件里面，文件会受不了的，解释一下参数信息：fileName后面如果后面不跟/，例如dev/logs/app.log，那就会把日志文件放在project工程下面，不是所属的项目下面如果后面跟/，例如/dev/logs/app.log，那就会把日志文件直接放在项目所在盘符的根目录下，例如项目在E盘存放，那就会把日志文件直接放在E盘的根目录下，如果后面直接加盘符，那就会存在特定的位置，例如F:/dev/logs/app.log,那就会直接放在F盘中特定的位置，上面都是经过测验的，fileName后面的app.log文件相当于是一个缓存文件，我们会把日志信息先放在app.log中，当达到我们设置的要求之后会把app.log中的日志信息转移到filePattern指定的日志文件中，转移的内容就会从app.log日志文件中清除，没有转移的内容还存放在app.log中，等到下一次符合要求的时候在进行一次转移-->
        <!--${r"${date:yyyy-MM}"}用在文件上面，输出的是目录的名字，例如2020-03，%d{MM-dd-yyyy}输入的就是月日年，例如03-02-2020，%i按照轮询输出，毕竟一天可能有符合要求的多个日志文件生成，所以需要在后面加一个类似于后缀的东西，当天的第一个日志文件可能是-1.log.gz，第二个文件就是-2.log.gz-->
        <RollingFile name="RollingFile" fileName="${r"${LOG_PATH}"}/${r"${LOG_FILE}"}.log" filePattern="${r"${LOG_PATH}"}/$${r"${date:yyyy-MM}"}/${r"${LOG_FILE}"}-%d{yyyy-MM-dd}-%i.log">
            <!--%thread:线程名;%-5level:级别从左显示5个字符宽度;%msg:在代码中需要输出的日志消息;%class{36}:估计显示的是完整类名-->
            <PatternLayout pattern="[dinky] %d{yyyy-MM-dd HH:mm:ss z} %-5level %class{36} %L %M - %msg%xEx%n"/>
            <!--<SizeBasedTriggeringPolicy size="300MB"/>-->
            <Policies>
                <!--TimeBasedTriggeringPolicy基于时间的触发策略，integer属性和上面<RollingFile>标签中的filePattern的值有关,例如：filePattern=”xxx%d{yyyy-MM-dd}xx” interval=”1” 表示将1天一个日志文件；filePattern=”xxx%d{yyyy-MM-dd-HH}xxx” interval=”1”表示一个小时一个日志文件,也就是说interval的单位取决于filePattern中的最小时间单位；modulate是（boolean）以0点钟为边界进行偏移计算，应该就是假设你中午启动项目，晚上0点也是一天了，而不是经过24小时才算一天-->
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
                <!--当app.log文件大小到达100MB的时候，就归档一次日志文件，也就是把app.log中的那前面100MB文件取出来，放到上面<RollingFile >中的filePattern后面的路径中-->
                <SizeBasedTriggeringPolicy size="100MB"/>
            </Policies>
        </RollingFile>
    </appenders>

    <loggers>
        <!--level="info"代表只能打印出info及其以上的信息；Console是上面Console标签的名字，往这一写，就可以往控制台上输出内容了，RollingFile是上面RollingFile标签的名字，往这一写，就会往设定的文件中输出内容了；当程序运行的时候就会被创建日志输出文件，不过里面没有任何日志内容，是否往里面输入日志，是通过下面的appender-ref标签控制的-->
        <root level="info">
            <appender-ref ref="Console"/>
            <!--一般不使用这个，只是让你知道有这个输出日志文件的方式而已-->
            <!--<appender-ref ref="File"/>-->
            <appender-ref ref="RollingFile"/>
        </root>
    </loggers>
</configuration>
