how to create a docker container that features the testrpc in memory chain and a solidity compiler

--- write docker file ---
see file Dockerfile in this git repository

--- build docker image ---
mzi@box ~/
$ docker build -t testrpc_solc .

--- start container ---
mzi@box ~/
$ docker run -it -p 8545:8545 -d testrpc_solc

--- test container using both testrpc + solc ---
mzi@box ~/
$ curl -X POST --data '{"jsonrpc":"2.0","method":"eth_compileSolidity","params":["pragma solidity ^0.4.0; contract test { function multiply(uint a) returns(uint d) { return a * 7; } }"],"id":1}' <ip-of-your-docker-box>:8545

this command should return someting along the following lines

{"id":1,"jsonrpc":"2.0","result":{"code":"0x6060604052346000575b60458060156000396000f3606060405260e060020a6000350463c6888fa18114601c575b6000565b346000576029600435603
b565b60408051918252519081900360200190f35b600781025b91905056","info":{"source":"pragma solidity ^0.4.0; contract test { function multiply(uint a) returns(uint d) { re
turn a * 7; } }","language":"Solidity","languageVersion":"0.4.6+commit.2dabbdf0.Emscripten.clang","compilerVersion":"0.4.6+commit.2dabbdf0.Emscripten.clang","abiDefi
nition":[{"constant":false,"inputs":[{"name":"a","type":"uint256"}],"name":"multiply","outputs":[{"name":"d","type":"uint256"}],"payable":false,"type":"function"}],"
userDoc":{"methods":{}},"developerDoc":{"methods":{}}}}}
