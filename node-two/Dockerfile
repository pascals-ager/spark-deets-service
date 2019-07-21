FROM base:advith
MAINTAINER Advith Nagappa

HEALTHCHECK CMD curl -f http://localhost:50075/ || exit 1

RUN mkdir -p /hadoop-data/dfs/datatwo
VOLUME /hadoop-data/dfs/datatwo

# Expose http port for datanode
EXPOSE 50075

# Expose ssh port
EXPOSE 2122

CMD ["/run.sh", "-d"]