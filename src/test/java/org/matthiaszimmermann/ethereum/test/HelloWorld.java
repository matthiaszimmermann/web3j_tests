package org.matthiaszimmermann.ethereum.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

public class HelloWorld {

	static final String CLIENT_URL = "http://192.168.99.100:8546";
	static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
	static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000L);

	static Web3j web3j = null;

	public static void main(String [] argv) throws Exception  {
		HelloWorld helloWorld = new HelloWorld();
		helloWorld.runHelloWorld();
	}

	public HelloWorld() {
		web3j = Web3j.build(new HttpService(CLIENT_URL));
	}

	public void runHelloWorld() throws Exception{
		Web3ClientVersion client = web3j.web3ClientVersion().sendAsync().get();
		System.out.println("Connected to " + client.getWeb3ClientVersion());
		
		EthCoinbase coinbase = web3j.ethCoinbase().sendAsync().get();
		EthAccounts accounts = web3j.ethAccounts().sendAsync().get();

		String address = accounts.getResult().get(1);
		BigDecimal balance = getBalance(address);
		BigInteger value = Convert.toWei("0.123", Convert.Unit.ETHER)
				.toBigInteger();
		
		String transactionHash = transfer(coinbase.getAddress(), address, value);
		waitForReceipt(transactionHash);

		System.out.println("Account " + address + ". " + 
				"Balance before Tx: " + balance + ", "  + 
				"balance after Tx: " + getBalance(address));
	}

	BigDecimal getBalance(String address) throws Exception {
		EthGetBalance balance = web3j
				.ethGetBalance(address, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		return Convert.fromWei(
				balance.getBalance().toString(), Convert.Unit.ETHER);
	}

	String transfer(String from, String to, BigInteger value) 
			throws Exception 
	{
		EthGetTransactionCount transactionCount = web3j
				.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		BigInteger nonce = transactionCount.getTransactionCount();
		Transaction transaction = Transaction.createEtherTransaction(
				from, nonce, GAS_PRICE, GAS_LIMIT, to, value);

		EthSendTransaction response = web3j
				.ethSendTransaction(transaction)
				.sendAsync()
				.get();

		return response.getTransactionHash();
	}

	TransactionReceipt waitForReceipt(String transactionHash) 
			throws Exception 
	{

		Optional<TransactionReceipt> receipt = getReceipt(transactionHash);
		int attempts = 40;

		while(attempts-- > 0 && !receipt.isPresent()) {
			Thread.sleep(1000);
			receipt = getReceipt(transactionHash);
		}

		if (attempts <= 0) {
			throw new RuntimeException("No Tx receipt received");
		}

		return receipt.get();
	}

	Optional<TransactionReceipt> getReceipt(String transactionHash) 
			throws Exception 
	{
		EthGetTransactionReceipt receipt = web3j
				.ethGetTransactionReceipt(transactionHash)
				.sendAsync()
				.get();

		return receipt.getTransactionReceipt();
	}
}
