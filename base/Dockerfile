FROM debian:8

RUN echo "deb [check-valid-until=no] http://cdn-fastly.deb.debian.org/debian jessie main" > /etc/apt/sources.list.d/jessie.list

RUN echo "deb [check-valid-until=no] http://archive.debian.org/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list

RUN sed -i '/deb http:\/\/deb.debian.org\/debian jessie-updates main/d' /etc/apt/sources.list

RUN apt-get -o Acquire::Check-Valid-Until=false update \
    && DEBIAN_FRONTEND=noninteractive apt-get -o Acquire::Check-Valid-Until=false install -t jessie-backports -y --no-install-recommends openjdk-8-jdk:amd64=8u171-b11-1~bpo8+1 \
    && DEBIAN_FRONTEND=noninteractive apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends net-tools=1.60-26+b1 curl=7.38.0-4+deb8u15 \
    && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends netcat=1.10-41 \
    && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends openssh-server=1:6.7p1-5+deb8u8 \
    && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends openssh-client=1:6.7p1-5+deb8u8 \
    && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends wget=1.16-1+deb8u6 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/

ENV HADOOP_VERSION 2.8.4
ENV HADOOP_URL https://www.apache.org/dist/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz
RUN set -x \
    && curl -fkSL "$HADOOP_URL" -o /tmp/hadoop.tar.gz \
    # && curl -fSL "$HADOOP_URL.asc" -o /tmp/hadoop.tar.gz.asc \
    # && gpg --verify /tmp/hadoop.tar.gz.asc \
    && tar -xvf /tmp/hadoop.tar.gz -C /opt/ \
    && rm /tmp/hadoop.tar.gz*

#RUN ln -s /opt/hadoop-$HADOOP_VERSION/etc/hadoop /etc/hadoop

RUN mkdir /opt/hadoop-$HADOOP_VERSION/logs

ENV HADOOP_PREFIX=/opt/hadoop-$HADOOP_VERSION
ENV HADOOP_COMMON_HOME=$HADOOP_PREFIX
ENV HADOOP_HDFS_HOME=$HADOOP_PREFIX
ENV HADOOP_MAPRED_HOME=$HADOOP_PREFIX
ENV HADOOP_YARN_HOME=$HADOOP_PREFIX
ENV HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
ENV YARN_CONF_DIR=$HADOOP_PREFIX/etc/hadoop

COPY /conf/mapred-site.xml $HADOOP_CONF_DIR/mapred-site.xml
COPY /conf/hdfs-site.xml $HADOOP_CONF_DIR/hdfs-site.xml
COPY /conf/core-site.xml $HADOOP_CONF_DIR/core-site.xml
COPY /conf/yarn-site.xml $HADOOP_CONF_DIR/yarn-site.xml
COPY /conf/slaves $HADOOP_CONF_DIR/slaves

ENV PATH $HADOOP_PREFIX/bin/:$HADOOP_PREFIX/sbin:$PATH
ENV PATH $PATH:$JAVA_HOME/bin

RUN sed -i '/^export JAVA_HOME/ s:.*:export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64\nexport HADOOP_PREFIX=/opt/hadoop-2.8.4\nexport HADOOP_HOME=/opt/hadoop-2.8.4\n:' $HADOOP_CONF_DIR/hadoop-env.sh
RUN sed -i '/^export HADOOP_CONF_DIR/ s:.*:export HADOOP_CONF_DIR=/opt/hadoop-2.8.4/etc/hadoop/:' $HADOOP_CONF_DIR/hadoop-env.sh

COPY config /root/.ssh/config
RUN chmod 600 /root/.ssh/config
RUN chown root:root /root/.ssh/config

RUN sed  -i "/^[^#]*UsePAM/ s/.*/#&/"  /etc/ssh/sshd_config
RUN echo "UsePAM no" >> /etc/ssh/sshd_config
RUN echo "Port 2122" >> /etc/ssh/sshd_config

# passwordless ssh
RUN ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys

COPY run.sh /run.sh
RUN chmod a+x /run.sh

ENV SPARK_VERSION 2.3.3
ENV SPARK_HADOOP_BUILD 2.7
ENV SPARK_URL https://www-eu.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop$SPARK_HADOOP_BUILD.tgz
RUN set -x \
    && curl -fkSL "$SPARK_URL" -o /tmp/spark.tar.tgz \
    #&& curl -fSL "$SPARK_URL.asc" -o /tmp/spark.tar.gz.asc \
    #&& gpg --verify /tmp/spark.tar.gz.asc \
    && tar -xvf /tmp/spark.tar.tgz -C /opt/ \
    && rm /tmp/spark.tar.tgz*

ENV SPARK_PREFIX=/opt/spark-$SPARK_VERSION-bin-hadoop$SPARK_HADOOP_BUILD
ENV SPARK_HOME=$SPARK_PREFIX
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HADOOP_PREFIX/lib/native

ENV PATH $PATH:$SPARK_PREFIX/bin

COPY /conf/spark-defaults.conf $SPARK_HOME/conf/spark-defaults.conf

RUN mkdir -p /tmp/exercise
COPY /exercise/brochure_clicks.json /tmp/exercise/brochure_clicks.json
COPY /exercise/brochures.json /tmp/exercise/brochures.json
COPY /exercise/page_turns.json /tmp/exercise/page_turns.json
COPY /exercise/enters.json /tmp/exercise/enters.json
COPY /exercise/exits.json /tmp/exercise/exits.json