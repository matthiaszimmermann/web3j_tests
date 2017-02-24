package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class DeployGreeterTest extends EthereumTest {
	
	@Test
	public void testDeployGreeter() throws Exception {
		
		final String MESSAGE = "hello world";

		// move funds to contract owner (amount in wei) to deploy the contract
		String coinbase = getAccount(0);
		String contractOwnerAdress = Alice.ADDRESS;
		System.out.println("Contract owner balance (initial): " + getBalance(contractOwnerAdress));

		BigInteger gasLimit = BigInteger.valueOf(1_500_000);
		BigInteger amount = gasLimit.multiply(GAS_PRICE);
		ensureFundsForTransaction(contractOwnerAdress, amount, BigInteger.valueOf(0));
		transferEther(coinbase, contractOwnerAdress, amount);
		System.out.println("Contract owner balance (pre-deploy): " + getBalance(contractOwnerAdress));

		// deploy the contract with the owner's credentials
		Credentials credentials = Alice.CREDENTIALS;
		BigInteger deployGasLimit = BigInteger.valueOf(300_000);
		BigInteger contractFundingAmount = BigInteger.valueOf(0); // BigInteger.valueOf(123_456); doesn't work as amount, TODO is this a testrpc thing? 
		Utf8String greeting = new Utf8String(MESSAGE);
		Future<Greeter> contractFuture = Greeter.deploy(web3j, credentials, GAS_PRICE, deployGasLimit, contractFundingAmount, greeting);
		Greeter contract = contractFuture.get();
		System.out.println("Contract owner balance (post-deploy): " + getBalance(contractOwnerAdress));

		// get contract address (after deploy)
		Assert.assertNotNull(contract);
		String contractAddress = contract.getContractAddress(); 
		Assert.assertTrue("Contract address does not start with '0x'", contractAddress.startsWith("0x"));
		System.out.println("Contract address balance (initial): " + getBalance(contractAddress));

		// move some funds to contract
		contractFundingAmount = BigInteger.valueOf(123_456);
		String txHash = transferEther(coinbase, contractAddress, contractFundingAmount);
		waitForTransactionReceipt(txHash);
		Assert.assertEquals("Contract address failed to receive proper amount of funds", contractFundingAmount, getBalance(contractAddress));
		System.out.println("Contract address balance (after transfer): " + getBalance(contractAddress));
		System.out.println("Contract owner balance (after transfer): " + getBalance(contractOwnerAdress));

		// call contract method greet()
		Future<Utf8String> messageFuture = contract.greet();
		Utf8String message = messageFuture.get();
		Assert.assertNotNull(message);
		Assert.assertEquals("Wrong message returned", MESSAGE, message.toString());
		System.out.println("Contract.greet(): " + message.toString());
		System.out.println("Contract address balance (after greet): " + getBalance(contractAddress));

		// kill contract
		BigInteger ownerBalanceBeforeKill = getBalance(contractOwnerAdress);
		Future<TransactionReceipt> txReceiptFuture = contract.kill();
		TransactionReceipt txReceipt = txReceiptFuture.get();
		Assert.assertNotNull(txReceipt);
		BigInteger ownerBalanceAfterKill = getBalance(contractOwnerAdress);
		BigInteger killFees = txReceipt.getCumulativeGasUsed().multiply(GAS_PRICE);
		System.out.println("Gas used (cumulative): " + txReceipt.getCumulativeGasUsed() + " kill fees: " + killFees);

		Assert.assertEquals("Bad contract owner balance after killing contract", ownerBalanceAfterKill, ownerBalanceBeforeKill.add(contractFundingAmount).subtract(killFees));
		System.out.println("Contract address balance (after kill): " + getBalance(contractAddress));
		System.out.println("Contract owner balance (after kill): " + getBalance(contractOwnerAdress));

		// try to run greet again (expect ExecutionException)
		exception.expect(ExecutionException.class);		
		messageFuture = contract.greet();
		try {
			message = messageFuture.get();
		}
		catch(Exception e) {
			System.out.println("Ok case: failed to call greet() on killed contract: " + e);
			throw e;
		}
	}
}
