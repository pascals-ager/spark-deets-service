The project comes bundled with images to spin up a test spark cluster to test our spark job:

## Set up a local development environment for Apache Spark(2.3.3) cluster with 3 Node Hadoop(2.8.4) using Yarn resource manager
How to create the test cluster
------------------------------
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
  
How to run the test cluster
---------------------------
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

192.0.2.2:50070  <---- hdfs overview

192.0.2.2:8088   <---- yarn cluster overview

## Build and test the spark job as a fat jar and submit it to the cluster using spark-submit
How to build and submit the spark fat jar using sbt:
----------------------------------------------------
1. Go to root of the project
2. sbt clean && sbt test &&sbt compile && sbt assembly
3. sbt assembly generates the fat jar @ /target/scala-2.11/user-deet-service.jar
4. For submitting the above jar to the previously created spark cluster, one must have a local spark (2.3) client lib installation
5. Say, @ /usr/local/spark/spark-2.3.3-bin-hadoop2.7
6. mkdir /usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf && cp -r base/conf /usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf to copy the configs for spark client && cd /usr/local/spark/spark-2.3.3-bin-hadoop2.7
6. The next steps are better performed as root, because the Docker cluster does not have proper user set up and is only meant for testing purposes
7. export YARN_CONF_DIR=/usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf/
8. ./bin/spark-submit --master yarn --deploy-mode cluster --queue default --class io.pascals.spark.UserDeetService user-deet-service.jar
9. The user_details (result dataset) is written out to hdfs /tmp and can be reviewed at http://node-master:50070/explorer.html#/tmp/user_details

## Assumption made about the data:
1. Page turns data is used to calculate total views
2. Page enters data is used to calculate total enters
3. Page exits data is used to calculate total exits

## Test results
1. sbt test Test are pretty slow. Can maybe put all tests into a single test case.
[info] UserDeetServiceTest:
[info] - ConfigsTest
[info] - Basic read counts
[info] - Events aggregate test
[info] - User Details test
[info] Run completed in 2 minutes, 42 seconds.
[info] Total number of tests run: 4
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 4, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 206 s, completed Jul 22, 2019 9:51:02 AM
2. sbt coverage coverageReport
[info] Statement coverage.: 50.51%
[info] Branch coverage....: 100.00%
[info] Coverage reports completed
[info] All done. Coverage was [50.51%]

## Results
Results can also be found at user_details.json

## Dependencies
sbt dependencyDot generates the dependency graph for the project
