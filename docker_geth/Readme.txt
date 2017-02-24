mzi@BSIM3236 ~/Desktop/private/github/web3j_tests/docker_geth (master)
$ docker build -t ethereum_geth_2 .

$ docker run -it -p 8545:8545 -d ethereum_geth_2 --password LocalPassword.txt --unlock 0 --mine --rpc --rpcaddr "0.0.0.0" --nodiscover –maxpeers 0

$ docker ps
CONTAINER ID        IMAGE               COMMAND
a24250f58021        ethereum_geth_2     "/geth --password Loc"

$ docker exec -it a24250f58021 bash

root@a24250f58021:/# /geth attach
Welcome to the Geth JavaScript console!

instance: Geth/v1.6.0-unstable-562ccff8/linux/go1.7.5
coinbase: 0xc9a5e0d1722e47dead4569e90306ed27ca7ae881
at block: 12 (Fri, 24 Feb 2017 13:58:43 UTC)
datadir: /root/.ethereum
modules: admin:1.0 debug:1.0 eth:1.0 miner:1.0 net:1.0 personal:1.0 rpc:1.0 txpool:1.0 web3:1.0
> net
{
  listening: true,
  peerCount: 0,
  version: "1",
  getListening: function(callback),
  getPeerCount: function(callback),
  getVersion: function(callback)
}
> eth.mining
true
> web3.fromWei(eth.getBalance(eth.coinbase), "ether")
0

... wait some time ...

> eth.blockNumber
50
> exit
root@a24250f58021:/# exit
exit


// geth console https://github.com/ethereum/go-ethereum/wiki/JavaScript-Console 
// more commands https://github.com/ethereum/wiki/wiki/JavaScript-API