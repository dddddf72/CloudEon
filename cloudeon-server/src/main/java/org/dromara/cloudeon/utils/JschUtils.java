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
package org.dromara.cloudeon.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.*;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.config.CloudeonConfigProp;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * hutool 默认封装的 SSH 工具，仅支持传入 SSL Private Key File，不支持  Private Key Content。
 * <p>
 * 本实现用于直接采用 Private Key Content 登录。
 *
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 2022/3/25
 * Time: 下午2:56
 * Email: zhzhenqin@163.com
 *
 * </pre>
 *
 * @author zhenqin
 */
@Slf4j
public class JschUtils {


    public final static String HEADER = "-----BEGIN RSA PRIVATE KEY-----";

    public final static String FOOTER = "-----END RSA PRIVATE KEY-----";

    /**
     * GETKEYTYPENAME_METHOD getKeyTypeName 是私有的
     *
     */
    private static final Method GET_KEY_TYPE_NAME_METHOD = ReflectUtil.getMethod(KeyPair.class, "getKeyTypeName");

    private static class ContentIdentity implements Identity {

        private KeyPair kpair;
        private final String identity;


        static ContentIdentity newInstance(byte[] prvContent, byte[] pubContent, String username, JSch jsch) throws Exception {
            KeyPair kpair = KeyPair.load(jsch, prvContent, pubContent);
            return new ContentIdentity(username, kpair);
        }

        private ContentIdentity(String name, KeyPair kpair) {
            this.identity = name;
            this.kpair = kpair;
        }


        /**
         * Decrypts this identity with the specified pass-phrase.
         *
         * @param passphrase the pass-phrase for this identity.
         * @return <tt>true</tt> if the decryption is succeeded
         * or this identity is not cyphered.
         */
        @Override
        public boolean setPassphrase(byte[] passphrase) {
            return kpair.decrypt(passphrase);
        }

        /**
         * Returns the public-key blob.
         *
         * @return the public-key blob
         */
        @Override
        public byte[] getPublicKeyBlob() {
            return kpair.getPublicKeyBlob();
        }

        /**
         * Signs on data with this identity, and returns the result.
         *
         * @param data data to be signed
         * @return the signature
         */
        @Override
        public byte[] getSignature(byte[] data) {
            return kpair.getSignature(data);
        }

        /**
         * @see #setPassphrase(byte[] passphrase)
         * @deprecated This method should not be invoked.
         */
        @Override
        public boolean decrypt() {
            throw new RuntimeException("not implemented");
        }

        /**
         * Returns the name of the key algorithm.
         *
         * @return "ssh-rsa" or "ssh-dss"
         */
        @Override
        public String getAlgName() {
            try {
                byte[] name = ReflectUtil.invoke(kpair, GET_KEY_TYPE_NAME_METHOD);
                return new String(name, CharsetUtil.CHARSET_UTF_8);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Returns the name of this identity.
         * It will be useful to identify this object in the {@link IdentityRepository}.
         */
        @Override
        public String getName() {
            return identity;
        }

        /**
         * Returns <tt>true</tt> if this identity is cyphered.
         *
         * @return <tt>true</tt> if this identity is cyphered.
         */
        @Override
        public boolean isEncrypted() {
            return kpair.isEncrypted();
        }

        /**
         * Disposes internally allocated data, like byte array for the private key.
         */
        @Override
        public void clear() {
            kpair.dispose();
            kpair = null;
        }

        /**
         * Returns an instance of {@link KeyPair} used in this {@link Identity}.
         *
         * @return an instance of {@link KeyPair} used in this {@link Identity}.
         */
        public KeyPair getKeyPair() {
            return kpair;
        }
    }


    /**
     * 通过私钥获取 Session
     *
     * @param sshHost    ssh 目标节点IP、域名、机器名
     * @param sshPort    ssh 服务端口号，一般为 22
     * @param sshUser    ssh 目标节点登录用户
     * @param privateKey 采用的私钥，一般由 ssh-keygen 生成。必须包含完整的前后缀：-----BEGIN RSA PRIVATE KEY-----
     * @param passphrase 密码
     * @return Session
     */
    public static Session createSession(String sshHost, int sshPort, String sshUser, String privateKey, byte[] passphrase) {
        final JSch jsch = new JSch();
        try {
            byte[] privateKeyByte = StrUtil.bytes(privateKey);
            Identity identity = ContentIdentity.newInstance(privateKeyByte, null, sshUser, jsch);
            jsch.addIdentity(identity, passphrase);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        return JschUtil.createSession(jsch, sshHost, sshPort, sshUser);
    }

    /**
     * ssh 执行模版命令(并自动删除执行后的命令脚本文件)
     *
     * @param charset  编码格式
     * @param session  会话
     * @param timeout  超时时间
     * @param function 回调，是为了保证在执行完成后能自动删除名
     * @param command  命令
     * @throws IOException io
     */
    public static void uploadCommandCallback(Session session, Charset charset, int timeout, Consumer<String> function, String command) throws IOException {
        if (StrUtil.isEmpty(command)) {
            return;
        }
        ChannelSftp channel = null;
        try {
            Sftp sftp = new Sftp(session, charset, timeout);
            channel = sftp.getClient();
            String tempId = IdUtil.fastSimpleUUID();
            File tmpDir = FileUtil.getTmpDir();
            File buildSsh = FileUtil.file(tmpDir, "ssh_temp", tempId + ".sh");
            CloudeonConfigProp cloudeonConfigProp = SpringUtil.getBean(CloudeonConfigProp.class);
            String templateScriptPath = cloudeonConfigProp.getRemoteScriptPath() + FileUtil.FILE_SEPARATOR + "template.sh";
            try (InputStream sshExecTemplateInputStream = new FileInputStream(templateScriptPath)) {
                String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
                FileUtil.writeString(sshExecTemplate + command + StrUtil.LF, buildSsh, charset);
            }
            // 上传文件
            String path = StrUtil.format("{}/.cloudeon/", sftp.home());
            String destFile = StrUtil.format("{}{}.sh", path, tempId);
            sftp.mkDirs(path);
            sftp.upload(destFile, buildSsh);
            // 执行命令
            try {
                String commandSh = "bash " + destFile;
                function.accept(commandSh);
            } finally {
                try {
                    // 删除 ssh 中临时文件
                    sftp.delFile(destFile);
                } catch (Exception e) {
                    log.warn("删除 ssh 临时文件失败", e);
                }
                // 删除临时文件
                FileUtil.del(buildSsh);
            }
        } finally {
            JschUtil.close(channel);
        }
    }

    /**
     * 执行命令回调
     *
     * @param session           会话
     * @param charset           字符编码格式
     * @param timeout           超时时间
     * @param lineHandler       消息回调
     * @param command           命令
     * @param commandParamsLine 执行参数
     * @throws IOException io
     */
    public static void execCallbackLine(Session session, Charset charset, int timeout, String command, String commandParamsLine, Map<String, String> env, LineHandler lineHandler) throws IOException {
        String command2 = formatStrByMap(command, env);
        execCallbackLine(session, charset, timeout, command2, commandParamsLine, lineHandler);
    }

    /**
     * 执行命令回调
     *
     * @param session           会话
     * @param charset           字符编码格式
     * @param timeout           超时时间
     * @param lineHandler       消息回调
     * @param command           命令
     * @param commandParamsLine 执行参数
     * @throws IOException io
     */
    public static void execCallbackLine(Session session, Charset charset, int timeout, String command, String commandParamsLine, LineHandler lineHandler) throws IOException {
        //
        execCallbackLine(session, charset, timeout, command, commandParamsLine, lineHandler, lineHandler);
    }

    /**
     * 执行命令回调
     *
     * @param session           会话
     * @param charset           字符编码格式
     * @param timeout           超时时间
     * @param normal            消息回调
     * @param error             错误消息
     * @param command           命令
     * @param commandParamsLine 执行参数
     * @throws IOException io
     */
    public static void execCallbackLine(Session session, Charset charset, int timeout, String command, String commandParamsLine, LineHandler normal, LineHandler error) throws IOException {
        //
        JschUtils.uploadCommandCallback(session, charset, timeout, (s) -> {
            ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand(StrUtil.bytes(s + StrUtil.SPACE + commandParamsLine, charset));
            channel.setInputStream(null);
            try {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    channel.setErrStream(outputStream, true);
                    channel.connect(timeout);
                    try (InputStream in = channel.getInputStream()) {
                        IoUtil.readLines(in, charset, normal);
                    }
                    // 输出错误信息
                    int size = outputStream.size();
                    if (size > 0) {
                        error.handle(outputStream.toString(charset.name()));
                    }
                } finally {
                    JschUtil.close(channel);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        }, command);
    }

    /**
     * 根据 map 替换 字符串变量
     *
     * @param str 字符串
     * @param evn map
     * @return 替换后
     */
    public static String formatStrByMap(String str, Map<String, String> evn) {
        String replace = str;
        Set<Map.Entry<String, String>> entries = evn.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            replace = StrUtil.replace(replace, StrUtil.format("${{}}", entry.getKey()), entry.getValue());
            replace = StrUtil.replace(replace, StrUtil.format("${}", entry.getKey()), entry.getValue());
        }
        return replace;
    }
}
