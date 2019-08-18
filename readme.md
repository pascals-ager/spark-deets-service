The project comes bundled with images to spin up a test spark cluster to test our spark job:

# Set up a local development environment for Apache Spark(2.3.3) cluster with 3 Node Hadoop(2.8.4) using Yarn resource manager

### How to create the test cluster
At the root of the repo:

docker-compose build

This will download all the tar sources required and create the images required to spin up the dev cluster. The sources however are not validated and a curl -k option is used to download them.

### Some Networking issues
Developers like to refer to containers by their name. To do so, these names have to be added to the /etc/hosts file (in the host machine ofcourse). The repo uses the private network "192.0.2.1/16".

Add below to /etc/hosts:

192.0.2.2	node-master

192.0.2.3	node-one

192.0.2.4	node-two

It always helps to ensure no other docker network occupies the above space. if they do, please clean them up (docker network prune and its more options) or choose another private network. 
  
### How to run the test cluster
At the root of the repo:

docker-compose up

### Services
 1. Hadoop namenode 
 2. Hadoop Datanodes x 2
 3. Yarn Resource Manager
 4. Yarn Node Managers x 2
 5. Spark 2.3.3(which uses Yarn for cluster management, allocation and shuffle)
 
Setting up a production cluster from this needs a lot more work. However you are ready to begin building data/ml applications.

192.0.2.2:50070  <---- hdfs overview

192.0.2.2:8088   <---- yarn cluster overview

# Build and test the spark job as a fat jar using sbt and submit it to the cluster using spark-submit

### How to build and submit the spark fat jar
 1. Go to root of the project
 2. sbt clean && sbt test && sbt assembly (will skip tests)
 3. sbt assembly generates the fat jar @ /target/scala-2.11/user-deet-service.jar
 4. For submitting the above jar to the previously created spark cluster, one must have a local spark (2.3) client lib installation
 5. Say, @ /usr/local/spark/spark-2.3.3-bin-hadoop2.7
 6. mkdir /usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf && cp -r base/conf /usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf to copy the configs for spark client && cd /usr/local/spark/spark-2.3.3-bin-hadoop2.7
 6. The next steps are better performed as root, because the Docker cluster does not have proper user set up and is only meant for testing purposes
 7. export YARN_CONF_DIR=/usr/local/spark/spark-2.3.3-bin-hadoop2.7/yarn/conf/
 8. ./bin/spark-submit --master yarn --deploy-mode cluster --queue default --class io.pascals.spark.UserDeetService user-deet-service.jar
 9. The user_details (result dataset) is written out to hdfs /tmp and can be reviewed at http://node-master:50070/explorer.html#/tmp/user_details

### Test results
```
sbt "set coverageEnabled := true" coverage test coverageReport
[info] UserDeetServiceTest:
[info] - ConfigsTest
[info] - Events aggregate test
[info] Run completed in 4 minutes, 51 seconds.
[info] Total number of tests run: 2
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 2, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 335 s, completed Jul 28, 2019 6:06:30 PM
[info] Waiting for measurement data to sync...
[info] Reading scoverage instrumentation [/home/scala/spark-deets-service/target/scala-2.11/scoverage-data/scoverage.coverage.xml]
[info] Reading scoverage measurements...
[info] Generating scoverage reports...
[info] Written Cobertura report [/home/scala/spark-deets-service/target/scala-2.11/coverage-report/cobertura.xml]
[info] Written XML coverage report [/home/scala/spark-deets-service/target/scala-2.11/scoverage-report/scoverage.xml]
[info] Written HTML coverage report [/home/scala/spark-deets-service/target/scala-2.11/scoverage-report/index.html]
[info] Statement coverage.: 74.78%
[info] Branch coverage....: 0.00%
[info] Coverage reports completed
[info] All done. Coverage was [74.78%]
```

There seems to be a bug with scoverage which adds runtime dependencies on spark uber jar. To avoid this, after a scoverageReport is
generated, it is important to clean and build the assembly once again.
Note: coverage is disabled by default.

### Results
Results can also be found at user_details.json

### Dependencies
sbt dependencyDot generates the dependency graph for the project

