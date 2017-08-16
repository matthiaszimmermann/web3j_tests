package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class DeployFibonacciTest extends EthereumTest {

	@Test
	public void testEventFilter() throws Exception {

		Fibonacci contract = deploy(Alice.CREDENTIALS);
		String contractAddress = contract.getContractAddress();

		Function function = createFibonacciFunction();
		String encodedFunction = FunctionEncoder.encode(function);

		BigInteger gas = estimateGas(encodedFunction);
		// TODO does not really use credentials but address only, needs an unlocked account ...
		String transactionHash = sendTransaction(Alice.CREDENTIALS, contractAddress, gas, encodedFunction);

		TransactionReceipt transactionReceipt =
				waitForTransactionReceipt(transactionHash);

		Assert.assertFalse("Transaction execution ran out of gas",
				gas.equals(transactionReceipt.getGasUsed()));

		List<Log> logs = transactionReceipt.getLogs();
		Assert.assertFalse(logs.isEmpty());

		Log log = logs.get(0);

		List<String> topics = log.getTopics();
		Assert.assertEquals(1, topics.size());

		Event event = new Event("Notify",
				Collections.emptyList(),
				Arrays.asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));

		// check function signature - we only have a single topic our event signature,
		// there are no indexed parameters in this example
		String encodedEventSignature = EventEncoder.encode(event);
		Assert.assertEquals(encodedEventSignature, topics.get(0));

		// verify our two event parameters
		List<Type> results = FunctionReturnDecoder.decode(
				log.getData(), event.getNonIndexedParameters());
		
		results.stream().forEach(t -> {System.out.println(t.getTypeAsString());});
		
//		assertThat(results, equalTo(Arrays.asList(
//				new Uint256(BigInteger.valueOf(7)), new Uint256(BigInteger.valueOf(13)))));

		// finally check it shows up in the event filter
		List<EthLog.LogResult> filterLogs = createFilterForEvent(
				encodedEventSignature, contractAddress);
		Assert.assertFalse(filterLogs.isEmpty());
	}

	private BigInteger estimateGas(String encodedFunction) throws Exception {
		EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(
				Transaction.createEthCallTransaction(null, encodedFunction))
				.sendAsync().get();
		// this was coming back as 50,000,000 which is > the block gas limit of 4,712,388 - see eth.getBlock("latest")
		return ethEstimateGas.getAmountUsed().divide(BigInteger.valueOf(100));
	}

	private String sendTransaction(
			Credentials credentials, String contractAddress, BigInteger gas,
			String encodedFunction) throws Exception 
	{
		String from = credentials.getAddress();
		BigInteger nonce = getNonce(from);
		
		// TODO create offline signed tx
		Transaction transaction = Transaction.createFunctionCallTransaction(
				from, nonce, Transaction.DEFAULT_GAS, gas, contractAddress,
				encodedFunction);

		org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =
				web3j.ethSendTransaction(transaction).sendAsync().get();

		Assert.assertFalse(transactionResponse.hasError());

		return transactionResponse.getTransactionHash();
	}

	private List<EthLog.LogResult> createFilterForEvent(
			String encodedEventSignature, String contractAddress) throws Exception {
		EthFilter ethFilter = new EthFilter(
				DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST,
				contractAddress
				);

		ethFilter.addSingleTopic(encodedEventSignature);

		EthLog ethLog = web3j.ethGetLogs(ethFilter).send();
		return ethLog.getLogs();
	}

	private Fibonacci deploy(Credentials ownerCredential) throws Exception {

		// deploy the contract with the owner's credentials
		BigInteger gasLimit = BigInteger.valueOf(400_000);
		BigInteger contractFunding = BigInteger.valueOf(0); 
		Fibonacci contract = Fibonacci.deploy(web3j, ownerCredential, GAS_PRICE, gasLimit, contractFunding).get();

		System.out.println("Contract deployed at " + contract.getContractAddress());

		return contract;
	}
	
    Function createFibonacciFunction() {
        return new Function(
                "fibonacciNotify",
                Collections.singletonList(new Uint(BigInteger.valueOf(7))),
                Collections.singletonList(new TypeReference<Uint>() {}));
    }
}
