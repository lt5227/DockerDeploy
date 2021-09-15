package org.stackstone.docker.deploy;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.stackstone.docker.deploy.command.BaseCommand;
import org.stackstone.docker.deploy.command.HadoopDockerCommand;
import org.stackstone.docker.deploy.util.AnsiUtil;

import java.util.Scanner;

/**
 * DockerDeployCommand
 *
 * @author Lt5227
 * @date 2021/9/14
 * @since 1.0.0
 */
public class DockerDeployCommand {
    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        System.out.println("Usage:");
        System.out.println("hadoop\t\t\tBuild hadoop docker image.");
        System.out.print(AnsiUtil.fgMsg(Ansi.Color.YELLOW, "Please enter the Docker image you want to build: ").reset());
        try(Scanner scanner = new Scanner(System.in)) {
            String build = scanner.next();
            BaseCommand command;
            if ("hadoop".equals(build)) {
                command = new HadoopDockerCommand();
                command.build();
            }
            System.out.println("Exit.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        AnsiConsole.systemUninstall();
    }
}
