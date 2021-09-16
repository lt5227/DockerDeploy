package org.stackstone.docker.deploy.command;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.XmlUtil;
import org.stackstone.docker.deploy.util.CommandExecUtil;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.stackstone.docker.deploy.util.AnsiUtil.fgMsg;

/**
 * HadoopDockerCmd
 * <br/>
 * Hadoop Docker 构建
 *
 * @author Lt5227
 * @date 2021 /9/14
 * @since 1.0.0
 */
public class HadoopDockerCommand implements BaseCommand {
    /**
     * The constant Y.
     */
    public static final String Y = "y";
    /**
     * The constant N.
     */
    public static final String N = "n";

    @Override
    public void build() {
        try (
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.print(fgMsg(YELLOW, "Please confirm whether to deploy the Hadoop service in the Docker container? (y or n): ").reset());
            String input = scanner.next();
            if (Y.equalsIgnoreCase(input)) {
                // Is Docker installed in the current system environment?
                String result = CommandExecUtil.execToString("docker version");
                if (CharSequenceUtil.isEmpty(result)) {
                    System.err.println("Docker is not installed in the local environment.");
                    return;
                }
                deleteOldHadoopDockerContainer();

                // User-defined image name.
                System.out.println(fgMsg(YELLOW, "Enter the name of the built image").reset());
                System.out.println(fgMsg(BLUE, "(eg: lee/hadoop:latest, input the character 'n' to use the default image name 'lt5227/hadoop:latest')").reset());
                System.out.print(fgMsg(YELLOW, "Please enter: ").reset());
                input = scanner.next();
                String imageName = "lt5227/hadoop:latest";
                if (!N.equalsIgnoreCase(input)) {
                    imageName = input;
                }
                System.out.println(fgMsg(GREEN, "The image named " + imageName).reset());
                deleteOldHadoopDockerImage(imageName);

                String path = System.getProperty("user.dir");
                File file = new File(path + File.separator + "dockerfile");
                String rootPath = file.getAbsolutePath();
                String dockerfilePath = rootPath + File.separator + "hadoop.Dockerfile";
                CommandExecUtil.execCmdAndPrint("docker build -f " + dockerfilePath + " -t " + imageName + " " + rootPath + ".");

                createDockerNetworkBridge();

                int nameNodeNum = 2, journeyNode = 3, nodeNum = 5;
                // Determine whether to deploy the HA mode
                System.out.print(fgMsg(YELLOW, "Whether to deploy HDFS in high availability mode?\n " +
                        "Note: If input 'Y', it will be configure and manage an HA HDFS cluster, " +
                        "using the Quorum Journal Manager (QJM) feature.\n" +
                        "Otherwise, a multi-node startup will have only one NameNode.\n" +
                        "If use HA mode, the NodeNumber at least is 5\n (Y / N): ").reset());
                input = scanner.next();
                boolean haMode = false;
                if (Y.equalsIgnoreCase(input)) {
                    haMode = true;
                    System.out.print(fgMsg(YELLOW, CYAN, "Whether use default node configuration?\nNote: If input 'Y', " +
                            "NodeNumber = 5, JourneyNode = 3, NameNode = 2\n(Y / N): ").reset());
                    input = scanner.next();
                    if (!Y.equalsIgnoreCase(input)) {
                        // Input starts several nodes.
                        System.out.print(fgMsg(YELLOW, "Please enter the number of nodes to start: ").reset());
                        nodeNum = Math.max(scanner.nextInt(), nodeNum);
                        // How many the name node?
                        System.out.print(fgMsg(YELLOW, "How many the name node to use? (The number ranges from 1 to nodeNum-3): ").reset());
                        nameNodeNum = Math.min(scanner.nextInt(), nodeNum - 3);
                        // How many the journey node?
                        System.out.print(fgMsg(YELLOW, "How many the journey node to use?\n" +
                                "\n(Note that when running with N JournalNodes, \nthe system can tolerate " +
                                "at most (N - 1) / 2 failures and continue to function normally.)\n:").reset());
                        journeyNode = Math.max(scanner.nextInt(), 3);
                    }
                }

                List<String> nodeNames = new ArrayList<>();
                for (int i = 1; i <= nodeNum; i++) {
                    String nodeName;
                    if (i < 10) {
                        nodeName = "hadoop-00" + i;
                    } else if (i < 100) {
                        nodeName = "hadoop-0" + i;
                    } else {
                        nodeName = "hadoop-" + i;
                    }
                    nodeNames.add(nodeName);
                }
                StringBuilder builder = new StringBuilder();
                nodeNames.forEach(s -> builder.append(s).append("\n"));

                if (haMode) {
                    // HA mode
                    // Modifying a Configuration File
                    /// core-site.xml

                    // Start each node first.
                    int port1 = 10022, port2 = 19870, port3 = 19000, port4 = 19864, port5 = 19866, port6 = 18088;
                    List<String> containerIds = new ArrayList<>();
                    for (int i = 0; i < nodeNames.size(); i++) {
                        String name = nodeNames.get(i);
                        if (i + 1 <= nameNodeNum) {
                            // Name Node
                            CommandExecUtil.execCmdAndPrint("docker run -itd -p " + port1 + ":22 -p " +
                                    port2 + ":9870 -p " + port3 + ":9000 -p " + port4 + ":9864 -p " +
                                    port5 + ":9866 -p " + port6 + ":8088 --network hadoop --name " +
                                    name + " --hostname " + name + " " + imageName);
                            port1 += 10000;
                            port2 += 10000;
                            port3 += 10000;
                            port4 += 10000;
                            port5 += 10000;
                            port6 += 10000;
                        } else {
                            CommandExecUtil.execCmdAndPrint("docker run -itd --network hadoop --name " +
                                    name + " --hostname " + name + " " + imageName);
                        }
                        String nodeContainerId = CommandExecUtil.execToString("docker ps -a -f \"name=" + name + "\" -q");
                        containerIds.add(nodeContainerId);
                        System.out.println(fgMsg(BLUE, "ContainerId: " + nodeContainerId + " started."));
                        // replacing workers configuration
                        CommandExecUtil.execCmdAndPrint("docker exec -i " + nodeContainerId + " su - hadoop -c " +
                                "\"echo '" + builder + "' > /home/hadoop/hadoop-3.2.2/etc/hadoop/workers\"");
                    }
                } else {
                    // normal mode
                    normalDeploy(imageName, nodeNames, builder);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
       Document document = XmlUtil.readXML(new File(Objects.requireNonNull(HadoopDockerCommand.class.getClassLoader().getResource("hadoop/core-site.xml")).getFile()));
        System.out.println(document);
    }

    private void normalDeploy(String imageName, List<String> nodeNames, StringBuilder builder) throws IOException {
        String masterContainerId = "";
        for (int i = 0; i < nodeNames.size(); i++) {
            String name = nodeNames.get(i);
            if (i == 0) {
                // Master node
                CommandExecUtil.execCmdAndPrint("docker run -itd -p 10022:22 -p 9870:9870 -p " +
                        "9000:9000 -p 9864:9864 -p 9866:9866 -p 8088:8088 --network hadoop --name " +
                        name + " --hostname " + name + " " + imageName);
            } else {
                // Slave node
                CommandExecUtil.execCmdAndPrint("docker run -itd --network hadoop --name " +
                        name + " --hostname " + name + " " + imageName);
            }
            String nodeContainerId = CommandExecUtil.execToString("docker ps -a -f \"name=" + name + "\" -q");
            if (i == 0) {
                masterContainerId = nodeContainerId;
            }
            System.out.println(fgMsg(BLUE, "ContainerId: " + nodeContainerId + " started."));
            // replacing workers configuration
            CommandExecUtil.execCmdAndPrint("docker exec -i " + nodeContainerId + " su - hadoop -c " +
                    "\"echo '" + builder + "' > /home/hadoop/hadoop-3.2.2/etc/hadoop/workers\"");
        }
        // Data init operate
        CommandExecUtil.execCmdAndPrint("docker exec -i " + masterContainerId +
                " su - hadoop -c \"hdfs namenode -format\"");
        // Start service
        CommandExecUtil.execCmdAndPrint("docker exec -i " + masterContainerId +
                " su - hadoop -c \"hadoop-3.2.2/sbin/start-dfs.sh && hadoop-3.2.2/sbin/start-yarn.sh\"");
    }

    private void createDockerNetworkBridge() throws IOException {
        // Create docker network bridge
        String networkId = CommandExecUtil.execToString("docker network ls -f \"name=hadoop\" -q");
        if (!CharSequenceUtil.isEmpty(networkId)) {
            System.out.println(fgMsg(GREEN, "Hadoop networkId: " + networkId + ". The network will be deleted.").reset());
            CommandExecUtil.execCmdAndPrint("docker network rm " + networkId);
        }
        CommandExecUtil.execCmdAndPrint("docker network create --driver=bridge hadoop");
    }

    private void deleteOldHadoopDockerImage(String imageName) throws IOException {
        // Get image id
        String imageId = CommandExecUtil.execToString("docker images -q -f \"reference=" + imageName + "\"");
        if (!CharSequenceUtil.isEmpty(imageId)) {
            System.out.println(fgMsg(GREEN, "The imageId of the " + imageName + " Image is " + imageId + ". The docker image will be deleted.").reset());
            CommandExecUtil.execCmdAndPrint("docker rmi " + imageId);
        }
    }

    private void deleteOldHadoopDockerContainer() throws IOException {
        String result;
        // Get the Docker container ID named starting with "hadoop-"
        System.out.println(fgMsg(GREEN, "Get the Docker container ID named starting with \"hadoop-\"").reset());
        String containerId = CommandExecUtil.execToString("docker ps -a -f \"name=hadoop-\" -q");
        // Stop and delete the Docker container.
        if (!CharSequenceUtil.isEmpty(containerId)) {
            containerId = containerId.replace("\n", " ");
            // Container exists, delete container
            System.out.println(fgMsg(GREEN, "The containerId is not empty.").reset());
            System.out.println(fgMsg(GREEN, "The containerIds: ").fg(WHITE).a(containerId).reset());
            System.out.println(fgMsg(GREEN, "Stop the container...").reset());
            result = CommandExecUtil.execToString("docker stop " + containerId);
            Arrays.stream(Objects.requireNonNull(result).split("\n")).forEach(s -> System.out.println("Container " + s + " has been stopped."));
            System.out.println(fgMsg(GREEN, "The container named starting with 'hadoop-' will be deleted.").reset());
            result = CommandExecUtil.execToString("docker rm " + containerId);
            Arrays.stream(Objects.requireNonNull(result).split("\n")).forEach(s -> System.out.println("Container " + s + " has been deleted."));
        }
    }
}
