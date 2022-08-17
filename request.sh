rm -f tmp
touch tmp

body=$(base64 --wrap=0 ./webserver/res/Ct0zUz-XgAAV69z.jpg)
body=$(echo "data:image/jpg;base64,$body")
curl -s -d $body localhost:8080/detectqrcode -o ./tmp
cat ./tmp | base64 -d > output.jpg
