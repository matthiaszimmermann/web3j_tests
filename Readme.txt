how to create a docker container that features the testrpc in memory chain and a solidity compiler

--- write docker file ---
see file Dockerfile in this git repository

--- build docker image ---
mzi@box ~/
$ eval $(docker-machine env default --shell=bash)

// change to directory that contains the Dockerfile
mzi@box ~/
$ docker build -t testrpc_solc .

// if docker build fails: restart the vm box and the docker deamon and try again 

$ docker images
REPOSITORY                                      TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
testrpc_solc                                    latest              d24ca6a8a2a7        13 days ago         602.5 MB

--- start container ---
mzi@box ~/
$ docker run -it -p 8545:8545 -d testrpc_solc

mzi@box ~/
$ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS                    NAMES
eb1bc60fb600        testrpc_solc        "testrpc"           13 days ago         Up 13 days          0.0.0.0:8545->8545/tcp   goofy_mc

--- test container using both testrpc + solc ---
mzi@box ~/
$ curl -X POST --data '{"jsonrpc":"2.0","method":"eth_compileSolidity","params":["pragma solidity ^0.4.0; contract test { function multiply(uint a) returns(uint d) { return a * 7; } }"],"id":1}' <ip-of-your-docker-box>:8545

this command should return someting along the following lines

{"id":1,"jsonrpc":"2.0","result":{"code":"0x6060604052346000575b60458060156000396000f3606060405260e060020a6000350463c6888fa18114601c575b6000565b346000576029600435603
b565b60408051918252519081900360200190f35b600781025b91905056","info":{"source":"pragma solidity ^0.4.0; contract test { function multiply(uint a) returns(uint d) { re
turn a * 7; } }","language":"Solidity","languageVersion":"0.4.6+commit.2dabbdf0.Emscripten.clang","compilerVersion":"0.4.6+commit.2dabbdf0.Emscripten.clang","abiDefi
nition":[{"constant":false,"inputs":[{"name":"a","type":"uint256"}],"name":"multiply","outputs":[{"name":"d","type":"uint256"}],"payable":false,"type":"function"}],"
userDoc":{"methods":{}},"developerDoc":{"methods":{}}}}}

--- test container using both testrpc and web3 javascript api

mzi@box ~/
$ docker ps
CONTAINER ID        IMAGE
b5c770d21bca        testrpc_solc

// enter running testrpc_solc container
mzi@box ~/
docker exec -it b5c770d21bca bash 

// in bash shell inside container: add node (should already be in the container though. TODO: check what's wrong ...)
root@b5c770d21bca:/# npm install web3

// start node from bash console
root@b5c770d21bca:/# node

// in node console: play around with web3 javascript api
var Web3 = require('web3');
var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider('http://localhost:8545'));
console.log(web3.isConnected());
console.log(web3.fromWei(web3.eth.gasPrice, 'wei').toString());
console.log(web3.eth.accounts);
console.log(web3.eth.coinbase);
console.log(web3.fromWei(web3.eth.getBalance(web3.eth.coinbase), 'wei').toString());
console.log(web3.fromWei(web3.eth.getBalance(web3.eth.coinbase), 'ether').toString());
