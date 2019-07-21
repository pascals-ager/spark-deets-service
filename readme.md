This repository creates a local development environment for Apache Spark(2.3.3) cluster with 3 Node Hadoop(2.8.4) using a Yarn resource manager
##############################################################################

How to build
------------
At the root of the repo:

docker-compose build

This will download all the tar sources required and create the images required to spin up the dev cluster. The sources however are not validated and a curl -k option is used to download them.

Some Networking issues
----------------------
Developers like to refer to containers by their name. To do so, these names have to be added to the /etc/hosts file (in the host machine ofcourse). The repo uses the private network "192.0.2.1/16".

Add below to /etc/hosts:

192.0.2.2	node-master

192.0.2.3	node-one

192.0.2.4	node-two

It always helps to ensure no other docker network occupies the above space. if they do, please clean them up (docker network prune and its more options) or choose another private network. 
  
How to run
----------
At the root of the repo:

docker-compose up


Services:
---------
1. Hadoop namenode 
2. Hadoop Datanodes x 2
3. Yarn Resource Manager
4. Yarn Node Managers x 2
5. Spark 2.3.3(which uses Yarn for cluster management, allocation and shuffle)
---------

Setting up a production cluster from this needs a lot more work. However you are ready to begin building data/ml applications.

node-master:50070  <---- hdfs overview

node-master:8088   <---- yarn cluster overview


