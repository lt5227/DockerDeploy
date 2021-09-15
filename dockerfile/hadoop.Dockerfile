# 以 CentOS8 镜像为基础生成新的镜像
FROM centos:centos8

# 指定维护者
MAINTAINER search

# passwd 命令安装
RUN yum -y install passwd && \
# sudo 命令安装
    yum -y install sudo && \
# 安装 ssh 客户端
    yum -y install openssh-clients

# JAVA 安装
## 创建 JAVA 目录
RUN mkdir /opt/java
## 将 JDK 放入 /opt/jdk 目录下
ADD install/jdk/jdk-8u301-linux-x64.tar.gz /opt/java

## 配置 JAVA 全局环境变量
RUN echo "export JAVA_HOME=/opt/java/jdk1.8.0_301" >> /etc/profile
RUN echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile
ENV JAVA_HOME /opt/java/jdk1.8.0_301
ENV PATH $JAVA_HOME/bin:$PATH

# 配置 ssh 远程登录
## 安装 openssh-server
RUN yum -y install openssh-server
## 指定 root 密码
RUN /bin/echo 'root:123456'|chpasswd
## 配置可以远程连接
RUN ssh-keygen -q -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N ''
RUN ssh-keygen -q -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N ''
RUN ssh-keygen -t dsa -f /etc/ssh/ssh_host_ed25519_key -N ''
RUN /bin/sed -i 's/.*session.*required.*pam_loginuid.so.*/session optional pam_loginuid.so/g' /etc/pam.d/sshd
RUN /bin/echo -e "LANG=\"en_US.UTF-8\"" > /etc/default/local
RUN echo "StrictHostKeyChecking ask" >> /etc/ssh/ssh_config

# 创建 hadoop 账号
RUN groupadd hadoop && useradd -d /home/hadoop -g hadoop -m hadoop
# 指定 hadoop 密码
RUN /bin/echo 'hadoop:123456'|chpasswd
# hadoop 账号赋予 sudo 权限
RUN /bin/echo 'hadoop ALL=(ALL) ALL' >> /etc/sudoers

# 设置 root 用户 ssh 免密登录
RUN rm /run/nologin && \
    ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys && \
    chmod 0600 ~/.ssh/authorized_keys

# 设置 hadoop 用户 ssh 免密登录
RUN su - hadoop -c "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa && cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys && chmod 0600 ~/.ssh/authorized_keys"

# 安装部署 hadoop 服务
ADD install/hadoop/hadoop-3.2.2.tar.gz /home/hadoop
## 编辑 hadoop 目录下的 etc/hadoop/hadoop-env.sh文件，定义JAVA_HOME
RUN sed -i '/# export JAVA_HOME=/a\export JAVA_HOME=/opt/java/jdk1.8.0_301' /home/hadoop/hadoop-3.2.2/etc/hadoop/hadoop-env.sh
## 设置 HADOOP_HOME 并添加环境变量
RUN echo "export HADOOP_HOME=/home/hadoop/hadoop-3.2.2" >> /etc/profile
RUN echo "export PATH=\$HADOOP_HOME/bin:\$PATH" >> /etc/profile
ENV HADOOP_HOME /home/hadoop/hadoop-3.2.2
ENV PATH $HADOOP_HOME/bin:$PATH
## 创建 hadoop.tmp.dir 目录
RUN su - hadoop -c "mkdir /home/hadoop/tmp"
## 上传配置文件并覆盖原有配置文件
### HDFS
ADD install/hadoop/config/core-site.xml /home/hadoop/hadoop-3.2.2/etc/hadoop
ADD install/hadoop/config/hdfs-site.xml /home/hadoop/hadoop-3.2.2/etc/hadoop
### YARN
ADD install/hadoop/config/mapred-site.xml /home/hadoop/hadoop-3.2.2/etc/hadoop
ADD install/hadoop/config/yarn-site.xml /home/hadoop/hadoop-3.2.2/etc/hadoop

# 暴露端口号
EXPOSE 22 9870 9000 9864 9866 8088

# 启动镜像，环境变量生效，容器启动会自动运行 ~/.bashrc
RUN echo "source /etc/profile" >> ~/.bashrc
# 格式化 HDFS 文件系统
# RUN su - hadoop -c "hdfs namenode -format"
# CMD su - hadoop -c "/home/hadoop/hadoop-3.2.2/sbin/start-dfs.sh && /home/hadoop/hadoop-3.2.2/sbin/start-yarn.sh" & /usr/sbin/sshd -D
CMD /usr/sbin/sshd -D