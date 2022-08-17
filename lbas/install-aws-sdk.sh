# if this file is failing to run, do chmod +x install-aws-sdk.sh
mkdir aws

curl "http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip" -o "aws-java-sdk.zip"
sudo unzip aws-java-sdk.zip -d aws

rm -r -f aws-*.zip