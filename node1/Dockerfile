FROM base:advith
MAINTAINER Advith Nagappa

HEALTHCHECK CMD curl -f http://localhost:50075/ || exit 1

RUN mkdir -p /hadoop-data/dfs/dataone
VOLUME /hadoop-data/dfs/dataone

# Expose http port for datanode
EXPOSE 50075

# Expose ssh port
EXPOSE 2122

CMD ["/run.sh", "-d"]