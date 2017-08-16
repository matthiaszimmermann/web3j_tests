package org.matthiaszimmermann.ethereum.test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import org.junit.Assert;

public class EthereumTest {

	public static final String CLIENT_IP = "192.168.99.100";
	public static final String CLIENT_PORT = "8545";

	// https://www.reddit.com/r/ethereum/comments/5g8ia6/attention_miners_we_recommend_raising_gas_limit/
	static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
	static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);

	private static final int SLEEP_DURATION = 15000;
	private static final int ATTEMPTS = 40;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	public static Web3j web3j = null;
	public static boolean setupFailed = false;

	//    public EthereumTest() {
	//        // HTTP Logging
	//        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
	//        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
	//        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
	//
	//        // IPC Logging
	////        System.setProperty("org.apache.commons.logging.simplelog.log.org.web3j.protocol.ipc", "DEBUG");
	//    }

	@Before
	public void setUp() throws Exception {
		String clientUrl = String.format("http://%s:%s", CLIENT_IP, CLIENT_PORT);
		web3j = Web3j.build(new HttpService(clientUrl));
	}

	TransactionReceipt waitForTransactionReceipt(
			String transactionHash) throws Exception {

		Optional<TransactionReceipt> transactionReceiptOptional =
				getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);

		if (!transactionReceiptOptional.isPresent()) {
			Assert.fail("Transaction receipt not generated after " + ATTEMPTS + " attempts");
		}

		TransactionReceipt txReceipt = transactionReceiptOptional.get();
		
		return txReceipt;
	}

	private Optional<TransactionReceipt> getTransactionReceipt(
			String transactionHash, int sleepDuration, int attempts) throws Exception 
	{
		Optional<TransactionReceipt> receiptOptional = sendTransactionReceiptRequest(transactionHash);

		for (int i = 0; i < attempts; i++) {
			if (!receiptOptional.isPresent()) {
				Thread.sleep(sleepDuration);
				receiptOptional = sendTransactionReceiptRequest(transactionHash);
			} 
			else {
				break;
			}
		}

		return receiptOptional;
	}

	private Optional<TransactionReceipt> sendTransactionReceiptRequest(
			String transactionHash) throws Exception 
	{
		EthGetTransactionReceipt transactionReceipt =
				web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

		return transactionReceipt.getTransactionReceipt();
	}

	void ensureFundsForTransaction(String address, BigInteger amount) throws Exception {
		BigInteger txFeeEstimate = GAS_PRICE.multiply(GAS_LIMIT);
		ensureFundsForTransaction(address, amount, txFeeEstimate);
	}
	
	void ensureFundsForTransaction(String address, BigInteger amount, BigInteger txFeeEstimate) throws Exception {
		BigInteger balance = getBalance(address);
		BigInteger totalAmount = amount.add(txFeeEstimate);
		BigInteger missingAmount = totalAmount.subtract(balance);
		
		if(balance.compareTo(totalAmount) >= 0) {
			return;
		}
		
		System.out.println(String.format("insufficient funds. transfer %d to %s from coinbase", missingAmount, address));
		
		transferFunds(address, missingAmount);
	}
	
	String transferFunds(String address, BigInteger amount) throws Exception {
		String txHash = transferEther(getCoinbase(), address, amount); 
		waitForTransactionReceipt(txHash);
		return txHash;
	}

	BigInteger getBalance(String address) throws Exception {
		EthGetBalance balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
		return balanceResponse.getBalance();
	}

	String transferEther(String from, String to, BigInteger amount) throws Exception {
		BigInteger nonce = getNonce(from);
		Transaction transaction = Transaction.createEtherTransaction(
				from, nonce, GAS_PRICE, GAS_LIMIT, to, amount);

		EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).sendAsync().get();
		
		// TODO remove
		System.out.println("transferEther. nonce: " + nonce + " amount: " + amount + " to: " + to);

		return ethSendTransaction.getTransactionHash();
	}

	BigInteger getNonce(String address) throws Exception {
		EthGetTransactionCount ethGetTransactionCount = 
				web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();

		return ethGetTransactionCount.getTransactionCount();
	}

	String getCoinbase() {
		return getAccount(0);
	}

	String getAccount(int i) {
		try {
			EthAccounts accountsResponse = web3j.ethAccounts().sendAsync().get();
			List<String> accounts = accountsResponse.getAccounts();

			return accounts.get(i);
		} 
		catch (Exception e) {
			System.out.println(e.getMessage());
			return "<no address>";
		}
	}

	static String load(String filePath) throws URISyntaxException, IOException {
		URL url = EthereumTest.class.getClass().getResource(filePath);
		byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
		return new String(bytes);
	}
}
