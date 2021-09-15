# 获取 Container Id
containerId=$(docker ps -a -f "name=hadoop-" -q)

## 判断是否生成容器，如果存在则执行删除
if [ -n "$containerId" ]; then
  printf "\033[33mThe containerId is not empty. \033[0m\n"
  printf "\033[35mThe container named starting with 'hadoop-' will be deleted.\033[0m\n"
  docker stop $containerId
  printf "\033[35mStopped\033[0m\n"
  docker rm $containerId
  printf "\033[35mDeleted\033[0m\n"
fi

# 用户输入命名Image的名称
printf "\033[33mEnter the name of the built image \n\033[0m\033[34m(eg: lee/hadoop:lastest, input the character 'n' to use the default image name 'lt5227/hadoop:latest')\033[0m\n"
printf "\033[33mPlease enter: \033[0m\033[5m"
read -r value
imageName=$value
if [ "$imageName" = 'n' ]; then
  imageName=lt5227/hadoop:latest
fi
printf "\033[33mThe image named %s \033[0m\n" "$imageName"

# 获取 Image Id
imageId=$(docker images -q -f "reference=$imageName:latest")

## 判断是否获取到镜像ID，如果存在则执行删除
if [ -n "$imageId" ]; then
  printf "\033[33m%s ImageID: %s. The docker image will be deleted \033[0m\n" "$imageName" "$imageId"
  docker rmi "$imageId"
fi

# 构建Docker镜像
printf "\033[42;34mDocker build image... \033[0m\n"
docker build -f ../dockerfile/hadoop.Dockerfile -t $imageName ../dockerfile

# 运行Docker镜像
printf "\033[42;34mDocker run image... \033[0m\n"
docker run -itd -p 10022:22 -p 9870:9870 -p 9000:9000 -p 9864:9864 -p 9866:9866 -p 8088:8088 --network hadoop --name hadoop-001 --hostname hadoop-001 "$imageName"

containerId=$(docker ps -a -f "name=hadoop-001" -q)

# 先执行数据格式化(数据初始化操作)
docker exec -i $containerId su - hadoop -c "hdfs namenode -format"
# 启动服务
docker exec -i $containerId su - hadoop -c "hadoop-3.2.2/sbin/start-dfs.sh && hadoop-3.2.2/sbin/start-yarn.sh"

# 运行完后不退出，输入字符后运行
#read -n 1

printf "\n\033[36mThe shell script has finished running.\033[0m\n\n"
# 倒计时5秒后退出
second=5
while [ $second -gt 0 ]; do
  printf "\033[32mThe window will be closed in %s \033[0m\n\r" "$second" | tr "\n" "\r"
  sleep 1
  second=$((second - 1))
done
