FROM ubuntu:14.04

MAINTAINER Matthias Zimmermann <matthias.zimmermann@bsi-software.com>

# windows workaround to ensure access to internet from within container
RUN echo "nameserver 8.8.8.8" >> /etc/resolv.conf

RUN apt-get update && apt-get install -y curl

# https://github.com/ethereumjs/testrpc: NOTE: As of version 3.0.2, testrpc requires at least Node 6.9.1 to run
RUN curl -sL https://deb.nodesource.com/setup_7.x | sudo -E bash -
RUN apt-get install -y nodejs python-pip python-dev build-essential git
RUN pip install --upgrade pip 
RUN pip install --upgrade virtualenv 
RUN npm install --unsafe-perm -g ethereumjs-testrpc
RUN npm install -g solc
RUN npm install -g web3

RUN apt-get clean

EXPOSE 8545

ENTRYPOINT ["testrpc"]
