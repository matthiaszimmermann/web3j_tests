package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class DeployMultiplyTest extends EthereumTest {
	
	public static final String MULTIPLY_CONTRACT =
			"pragma solidity ^0.4.0;\n" + 
					"contract test {\n" +
					"    function multiply(uint a) returns(uint d) {\n" +
					"        return a * 7;\n" +
					"    }\n" + 
					"}";

	public static final String MULTIPLY_CONTRACT_COMPILED = "0x6060604052346000575b60458060156000396000f3606060405260e060020a6000350463c6888fa18114601c575b6000565b346000576029600435603b565b60408051918252519081900360200190f35b600781025b91905056";
	
	@Test
	public void verifyDeployMultiply() throws Exception {
		String coinbase = getCoinbase();
		BigInteger nonce = getNonce(coinbase);
		BigInteger gasPrice = GAS_PRICE;
		String compiledContract = MULTIPLY_CONTRACT_COMPILED;

		// TODO consider to work with raw transaction + offline tx handling
		// BigInteger gasLimit = GAS_LIMIT_DEFAULT;
		// BigInteger value = BigInteger.ZERO;
		// RawTransaction rawTransaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, value, compiledContract);

		Transaction transaction = Transaction.createContractTransaction(
				coinbase,
				nonce,
				gasPrice,
				compiledContract
				);

		EthSendTransaction transactionResponse
		= web3j.ethSendTransaction(transaction).sendAsync().get();

		Assert.assertNotNull(transactionResponse);

		String transactionHash = transactionResponse.getTransactionHash();
		Assert.assertTrue("Tx does not start with '0x'", transactionHash.startsWith("0x"));
		System.out.println("Tx hash: " + transactionHash);
		TransactionReceipt transactionReceipt = waitForTransactionReceipt(transactionHash);

		String contractAddress = transactionReceipt.getContractAddress();
		Assert.assertTrue("Contract address not start with '0x'", contractAddress.startsWith("0x"));
		System.out.println("Contract address:" + contractAddress);

		BigInteger outputValue = callMultiply(contractAddress, BigInteger.valueOf(4));
		Assert.assertEquals("Bad contract.multiply value", BigInteger.valueOf(28), outputValue);
		System.out.println("Multiply function call output: " + outputValue);
	}

	private BigInteger callMultiply(String contractAddress, BigInteger value) throws Exception {
		Function multiply = new Function("multiply",
				Collections.singletonList(new Uint(value)),
				Collections.singletonList(new TypeReference<Uint>() {}));

		String responseValue = callSmartContractFunction(multiply, contractAddress);

		List<Type> output = FunctionReturnDecoder.decode(
				responseValue, multiply.getOutputParameters()
				);

		BigInteger outputValue = (BigInteger) output.get(0).getValue();

		return outputValue;
	}

	private String callSmartContractFunction(
			Function function, String contractAddress) throws Exception {

		String encodedFunction = FunctionEncoder.encode(function);

		org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
				Transaction.createEthCallTransaction(contractAddress, encodedFunction),
				DefaultBlockParameterName.LATEST)
				.sendAsync().get();

		return response.getValue();
	}	

}
