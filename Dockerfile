# 使用JDK 17作为基础镜像
FROM amazoncorretto:17

# 维护者信息
LABEL maintainer="lxsky711@qq.com"

# 添加变量，这些可以在运行容器时通过--env选项覆盖
ENV MYSQL_USERNAME=xdechat \
    MYSQL_PASSWORD=ZgDNPToXvd%YVZ!W \
    MYSQL_URL=jdbc:mysql://47.121.118.249:3306/xdechat?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true

# 将应用的jar包添加到容器中
ADD target/xdu-chat-0.0.1-SNAPSHOT.jar xdu-chat.jar

# 暴露端口
EXPOSE 8448

# 运行jar文件，并通过环境变量传递数据库配置
ENTRYPOINT ["java","-jar","/xdu-chat.jar", "--spring.datasource.username=${MYSQL_USERNAME}", "--spring.datasource.password=${MYSQL_PASSWORD}", "--spring.datasource.url=${MYSQL_URL}"]
