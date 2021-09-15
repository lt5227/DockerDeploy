package org.stackstone.docker.deploy.util;

import cn.hutool.core.text.CharSequenceUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.exec.ExecuteWatchdog.INFINITE_TIMEOUT;

/**
 * CommandExecUtil
 *
 * @author Lt5227
 * @date 2021 /9/14
 * @since 1.0.0
 */
public class CommandExecUtil {

    /**
     * Execute the command to return the execution result string
     *
     * @param command the command
     * @return result string
     */
    public static String execToString(String command) throws IOException {
        if (CharSequenceUtil.isNotEmpty(command)) {
            try (
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ) {
                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream);
                execCmd(command, null, streamHandler);
                String result = outputStream.toString("UTF-8");
                return CharSequenceUtil.isNotEmpty(result) ? result.trim() : result;
            }
        }
        return null;
    }

    public static int execCmdAndPrint(final String line) throws IOException {
        CommandLine commandLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(null);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(INFINITE_TIMEOUT);
        executor.setWatchdog(watchdog);
        return executor.execute(commandLine);
    }

    public static int execCmd(final String line, PumpStreamHandler streamHandler) throws IOException {
        return execCmd(line, null, streamHandler);
    }

    public static int execCmd(final String line, final Map<String, ?> substitutionMap, PumpStreamHandler streamHandler) throws IOException {
        CommandLine commandLine = CommandLine.parse(line, substitutionMap);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(null);
        if (streamHandler != null) {
            executor.setStreamHandler(streamHandler);
        }
        // exit code: 0 = success, 1 = error
        return executor.execute(commandLine);
    }

    public static void main(String[] args) throws IOException {
        String result = execToString("docker ps -a -f \"name=hadoop-\" -q");
        System.out.println(result);
        File file = new File("dockerfile/hadoop.Dockerfile");
        System.out.println(file);
    }
}
