FROM base:advith
MAINTAINER Advith Nagappa

HEALTHCHECK CMD curl -f http://localhost:50070/ || exit 1

RUN mkdir -p /hadoop-data/dfs/name
VOLUME /hadoop-data/dfs/name

# Expose http port for namenode
EXPOSE 50070

# Expose http port for yarn
EXPOSE 8088

# Expose ssh port
EXPOSE 2122

# Expose namenode fs port
EXPOSE 8020

# Expose yarm rm port
EXPOSE 8030

# Expose yarn resource tracker port
EXPOSE 8031

# Expose yarn scheduler port
EXPOSE 8032

# Expose spark history server http port
EXPOSE 18080

CMD ["/run.sh", "-d"]