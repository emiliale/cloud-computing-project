#javac -cp aws/aws-java-sdk-1.12.230/lib/aws-java-sdk-1.12.230.jar:aws/aws-java-sdk-1.12.230/third-party/lib/*:src src/main/java/pt/ulisboa/tecnico/cnv/lbas/**.java
#java -cp aws/aws-java-sdk-1.12.230/lib/aws-java-sdk-1.12.230.jar:aws/aws-java-sdk-1.12.230/third-party/lib/*:src/main/java pt.ulisboa.tecnico.cnv.lbas.WebServer

mvn clean package
java -cp aws/aws-java-sdk-1.12.230/lib/aws-java-sdk-1.12.230.jar:aws/aws-java-sdk-1.12.230/third-party/lib/*:target/lbas-1.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.lbas.WebServer
