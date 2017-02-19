package org.matthiaszimmermann.ethereum.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;
import org.web3j.protocol.core.methods.response.EthCompileSolidity.Code;
import org.web3j.protocol.core.methods.response.EthCompileSolidity.SolidityInfo;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetCompilers;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.NetPeerCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Unit test for simple App.
 */
public class AppTest {

	public static final String CLIENT_IP = "192.168.99.100";
	public static final String CLIENT_PORT = "8545";

	public static final String MULTIPLY_CONTRACT =
			"pragma solidity ^0.4.0;\n" + 
					"contract test {\n" +
					"    function multiply(uint a) returns(uint d) {\n" +
					"        return a * 7;\n" +
					"    }\n" + 
					"}";

	public static final String MULTIPLY_CONTRACT_COMPILED = "0x6060604052346000575b60458060156000396000f3606060405260e060020a6000350463c6888fa18114601c575b6000565b346000576029600435603b565b60408051918252519081900360200190f35b600781025b91905056";

	/**
	 * greeter contract inspired by
	 * https://www.ethereum.org/greeter
	 */
	public static final String GREETER_CONTRACT_SIMPLE = 
			"pragma solidity ^0.4.0;\n" + 
					"contract greeter {\n" +
					"    address owner;\n" +
					"    string greeting;\n" +
					"    /* this runs when the contract is executed */\n" +
					"    function greeter(string _greeting) {\n" +
					"        greeting = _greeting;  \n" +
					"        owner = msg.sender; \n" +
					"    }\n" +
					"    /* main function */\n" +
					"    function greet() constant returns (string) {\n" +
					"        return greeting;\n" +
					"    }\n" +
					"    function kill() { if (msg.sender == owner) selfdestruct(owner); }\n" +
					"}";

	public static final String GREETER_CONTRACT_SIMPLE_COMPILED = "0x60606040523461000057604051610282380380610282833981016040528051015b8060019080519060200190828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061006c57805160ff1916838001178555610099565b82800160010185558215610099579182015b8281111561009957825182559160200191906001019061007e565b5b506100ba9291505b808211156100b657600081556001016100a2565b5090565b505060008054600160a060020a0319166c01000000000000000000000000338102041790555b505b610192806100f06000396000f3606060405260e060020a600035046341c0e1b58114610029578063cfae321714610038575b610000565b34610000576100366100b3565b005b34610000576100456100f5565b60405180806020018281038252838181518152602001915080519060200190808383829060006004602084601f0104600302600f01f150905090810190601f1680156100a55780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000543373ffffffffffffffffffffffffffffffffffffffff908116911614156100f25760005473ffffffffffffffffffffffffffffffffffffffff16ff5b5b565b604080516020808201835260008252600180548451600282841615610100026000190190921691909104601f8101849004840282018401909552848152929390918301828280156101875780601f1061015c57610100808354040283529160200191610187565b820191906000526020600020905b81548152906001019060200180831161016a57829003601f168201915b505050505090505b9056";

	/**
	 * gas price: check <a href="https://ethstats.net/">ethstats.net</a> for current values. For additional infor see <a
	 * href="http://ethereum.stackexchange.com/questions/1113/can-i-set-the-gas-price-to-whatever-i-want/1133>ethereum.stackexchange.com</a>
	 */
	public static final BigInteger GAS_PRICE_DEFAULT = BigInteger.valueOf(20_000_000_000L);
	public static final BigInteger GAS_LIMIT_DEFAULT = BigInteger.valueOf(30_000L);
	public static final BigInteger TX_FEE_DEFAULT = GAS_PRICE_DEFAULT.multiply(GAS_LIMIT_DEFAULT);

	private static final int SLEEP_DURATION = 15000;
	private static final int ATTEMPTS = 40;

	// copied from org.web3j.protocol.core.methods.response.Numeric
	private static final String HEX_PREFIX = "0x";

	private static Web3j web3 = null;
	private static boolean setupFailed = false;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@BeforeClass
	public static void setUp() {
		try {
			String clientUrl = String.format("http://%s:%s", CLIENT_IP, CLIENT_PORT);
			web3 = Web3j.build(new HttpService(clientUrl));
		} 
		catch (Exception e) {
			setupFailed = true;
		}
	}

	@Test
	public void verifySetup() {
		Assert.assertTrue("connection to local ethereum client failed", !setupFailed);
	}

	@Test
	public void verifyClientVersion() throws Exception {
		if(setupFailed) {
			return;
		}

		Web3ClientVersion versionResponse = web3.web3ClientVersion().sendAsync().get();
		String version = versionResponse.getWeb3ClientVersion();

		Assert.assertTrue(String.format("client version '%s' doesn't contain ethereum", version), version.toLowerCase().contains("ethereum"));
	}

	/**
	 * check peers
	 */
	@Test
	public void verifyPeerCount() throws Exception {
		if(setupFailed) {
			return;
		}

		NetPeerCount peersResponse = web3.netPeerCount().sendAsync().get();
		String peers = peersResponse.getResult();

		Assert.assertEquals("peers is not '0'", "0", peers);
	}

	/**
	 * check coinbase of the local client
	 */
	@Test
	public void verifyCoinbase() throws Exception {
		if(setupFailed) {
			return;
		}

		EthCoinbase coinbase = web3.ethCoinbase().sendAsync().get();
		String address = coinbase.getAddress();

		isValidAddress(address, "coinbase");
	}

	/**
	 * check coinbase has some funds.
	 * may be verified against web3 value in node as follows:
	 * var Web3 = require('web3');
	 * var web3 = new Web3();
	 * web3.setProvider(new web3.providers.HttpProvider('http://localhost:8545'));
	 * console.log(web3.isConnected());
	 * console.log(web3.fromWei(web3.eth.gasPrice, 'wei').toString());
	 * console.log(web3.eth.accounts);
	 * console.log(web3.eth.coinbase);
	 * console.log(web3.fromWei(web3.eth.getBalance(web3.eth.coinbase), 'wei').toString());
	 * console.log(web3.fromWei(web3.eth.getBalance(web3.eth.coinbase), 'ether').toString());
	 */
	@Test
	public void verifyCoinbaseBalance() throws Exception {
		if(setupFailed) {
			return;
		}

		EthCoinbase coinbase = web3.ethCoinbase().sendAsync().get();
		hasWeis(coinbase.getAddress(), BigInteger.valueOf(100_000_000_000_000_000L));
	}

	@Test
	public void verifyCoinbaseNonce() throws Exception {
		if(setupFailed) {
			return;
		}

		EthCoinbase coinbase = web3.ethCoinbase().sendAsync().get();
		String address = coinbase.getAddress();
		BigInteger nonce = getNonce(address);

		System.out.println(String.format("nonce: %d account: %s" , nonce, address));
	}

	@Test
	public void verifySolidityCompilerAvailable() throws Exception {
		if(setupFailed) {
			return;
		}

		EthGetCompilers compilerResponse = web3.ethGetCompilers().sendAsync().get();
		List<String> compiler = compilerResponse.getCompilers();

		compiler.stream().forEach(c -> System.out.println("web3 compiler[] " + c));

		Assert.assertTrue("access to solidity compiler missing", compiler.contains("solidity"));
	}

	@Test
	public void verifyCompileMultiplyRpc20() throws Exception {
		if(setupFailed) {
			return;
		}

		String sourceCode = MULTIPLY_CONTRACT;
		JsonObject result = compileSolidityCode(sourceCode);		
		Assert.assertNotNull(result);

		JsonObject info = result.get("info").getAsJsonObject();
		Assert.assertNotNull(info);

		String codeCompiled = result.get("code").toString().replaceAll("\"", "");
		String sourceOut = info.get("source").toString().replaceAll("\"", "");
		String abiDefinition = info.get("abiDefinition").toString();

		compareLongStrings(normalizeSoliditySource(MULTIPLY_CONTRACT), sourceOut);
		compareLongStrings(MULTIPLY_CONTRACT_COMPILED, codeCompiled);

		Assert.assertEquals("compiled code does not match", MULTIPLY_CONTRACT_COMPILED, codeCompiled);
		Assert.assertEquals("return source code does not match", normalizeSoliditySource(MULTIPLY_CONTRACT), sourceOut);
		Assert.assertNotNull(abiDefinition);
	}

	@Test
	public void verifyCompileGreeterRpc20() throws Exception {
		if(setupFailed) {
			return;
		}

		String sourceCode = GREETER_CONTRACT_SIMPLE;
		JsonObject result = compileSolidityCode(sourceCode);		
		Assert.assertNotNull(result);

		JsonObject info = result.get("info").getAsJsonObject();
		Assert.assertNotNull(info);

		String codeCompiled = result.get("code").toString().replaceAll("\"", "");
		String sourceOut = info.get("source").toString().replaceAll("\"", "");
		String abiDefinition = info.get("abiDefinition").toString();

		compareLongStrings(normalizeSoliditySource(GREETER_CONTRACT_SIMPLE), sourceOut);
		compareLongStrings(GREETER_CONTRACT_SIMPLE_COMPILED, codeCompiled);

		Assert.assertEquals("compiled code does not match", GREETER_CONTRACT_SIMPLE_COMPILED, codeCompiled);
		Assert.assertEquals("return source code does not match", normalizeSoliditySource(GREETER_CONTRACT_SIMPLE), sourceOut);
		Assert.assertNotNull(abiDefinition);
	}

	private void compareLongStrings(String expected, String actual) {
		System.out.println(String.format("exp %4d %s", expected.length(), expected));
		System.out.println(String.format("act %4d %s", actual.length(), actual));
		System.out.print("         ");

		for(int i = 0; i < expected.length() && i < actual.length(); i++) {
			System.out.print(expected.charAt(i) == actual.charAt(i) ? "." : "!");
		}

		System.out.println();
	}

	@Test
	public void verifyDeployGreeter() throws Exception {
		if(setupFailed) {
			return;
		}

		final String MESSAGE = "hello world";


		// move funds to contract owner (amount in wei) to deploy the contract
		String coinbase = getAccount(0);
		String contractOwnerAdress = SampleKeys.ADDRESS;
		System.out.println("contract owner balance (initial): " + getBalance(contractOwnerAdress));

		BigInteger amount = GAS_PRICE_DEFAULT.multiply(BigInteger.valueOf(1_500_000));
		transferEther(coinbase, contractOwnerAdress, amount);
		System.out.println("contract owner balance (pre-deploy): " + getBalance(contractOwnerAdress));

		// deploy the contract with the owner's credentials
		Credentials credentials = SampleKeys.CREDENTIALS;
		BigInteger deployGasLimit = BigInteger.valueOf(300_000);
		BigInteger contractFundingAmount = BigInteger.valueOf(0); // BigInteger.valueOf(123_456); doesn't work as amount, TODO is this a testrpc thing? 
		Utf8String greeting = new Utf8String(MESSAGE);
		Future<Greeter> contractFuture = Greeter.deploy(web3, credentials, GAS_PRICE_DEFAULT, deployGasLimit, contractFundingAmount, greeting);
		Greeter contract = contractFuture.get();
		System.out.println("contract owner balance (post-deploy): " + getBalance(contractOwnerAdress));

		// get contract address (after deploy)
		Assert.assertNotNull(contract);
		String contractAddress = contract.getContractAddress(); 
		Assert.assertTrue("contract address does not start with '0x'", contractAddress.startsWith("0x"));
		System.out.println("contract address balance (initial): " + getBalance(contractAddress));

		// move some funds to contract
		contractFundingAmount = BigInteger.valueOf(123_456);
		transferEther(coinbase, contractAddress, contractFundingAmount);
		Assert.assertEquals("contract address failed to receive proper amount of funds", contractFundingAmount, getBalance(contractAddress));
		System.out.println("contract address balance (after transfer): " + getBalance(contractAddress));
		System.out.println("contract owner balance (after transfer): " + getBalance(contractOwnerAdress));

		// call contract method greet()
		Future<Utf8String> messageFuture = contract.greet();
		Utf8String message = messageFuture.get();
		Assert.assertNotNull(message);
		Assert.assertEquals("wrong message returned", MESSAGE, message.toString());
		System.out.println("contract.greet(): " + message.toString());
		System.out.println("contract address balance (after greet): " + getBalance(contractAddress));

		// kill contract
		BigInteger ownerBalanceBeforeKill = getBalance(contractOwnerAdress);
		Future<TransactionReceipt> txReceiptFuture = contract.kill();
		TransactionReceipt txReceipt = txReceiptFuture.get();
		Assert.assertNotNull(txReceipt);
		BigInteger ownerBalanceAfterKill = getBalance(contractOwnerAdress);
		BigInteger killFees = txReceipt.getCumulativeGasUsed().multiply(GAS_PRICE_DEFAULT);
		System.out.println("gas used (cumulative): " + txReceipt.getCumulativeGasUsed() + " kill fees: " + killFees);

		Assert.assertEquals("bad contract owner balance after killing contract", ownerBalanceAfterKill, ownerBalanceBeforeKill.add(contractFundingAmount).subtract(killFees));
		System.out.println("contract address balance (after kill): " + getBalance(contractAddress));
		System.out.println("contract owner balance (after kill): " + getBalance(contractOwnerAdress));

		// try to run greet again (expect ExecutionException)
		exception.expect(ExecutionException.class);		
		messageFuture = contract.greet();
		try {
			message = messageFuture.get();
		}
		catch(Exception e) {
			System.out.println("ok case: failed to call greet() on killed contract: " + e);
			throw e;
		}
	}

	@Test
	public void verifyDeployMultiply() throws Exception {
		if(setupFailed) {
			return;
		}

		String coinbase = getAccount(0);
		BigInteger nonce = getNonce(coinbase);
		BigInteger gasPrice = GAS_PRICE_DEFAULT;
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
		= web3.ethSendTransaction(transaction).sendAsync().get();

		Assert.assertNotNull(transactionResponse);

		String transactionHash = transactionResponse.getTransactionHash();
		Assert.assertTrue("tx does not start with '0x'", transactionHash.startsWith("0x"));
		System.out.println("tx hash: " + transactionHash);
		TransactionReceipt transactionReceipt = waitForTransactionReceipt(transactionHash);

		String contractAddress = transactionReceipt.getContractAddress();
		Assert.assertTrue("contract address not start with '0x'", contractAddress.startsWith("0x"));
		System.out.println("contract address:" + contractAddress);

		BigInteger outputValue = callMultiply(contractAddress, BigInteger.valueOf(4));
		Assert.assertEquals("bad contract.multiply value", BigInteger.valueOf(28), outputValue);
		System.out.println("multiply function call output: " + outputValue);
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

		org.web3j.protocol.core.methods.response.EthCall response = web3.ethCall(
				Transaction.createEthCallTransaction(contractAddress, encodedFunction),
				DefaultBlockParameterName.LATEST)
				.sendAsync().get();

		return response.getValue();
	}	

	// TODO verify compiling of contract with syntax bugs

	// TODO there is still a bug for this (probably something wrong with testrpc vs the real thing ...)
	@Test
	public void verifySolidityCompileGreeter() throws Exception {
		if(setupFailed) {
			return;
		}

		EthCompileSolidity compiledRequest = web3.ethCompileSolidity(normalizeSoliditySource(GREETER_CONTRACT_SIMPLE)).sendAsync().get();
		Assert.assertTrue("unexpected compiling error", !compiledRequest.hasError());

		Map<String, Code> codeMap = compiledRequest.getCompiledSolidity(); 

		Assert.assertNotNull(codeMap);

		for(String key: codeMap.keySet()) {
			Code codeObject = codeMap.get(key);
			Assert.assertNotNull(codeObject);

			SolidityInfo solidityInfo = codeObject.getInfo();
			String code = codeObject.getCode();

			System.out.println(solidityInfo.toString() + ": " + code);
		}
	}

	private BigInteger hasWeis(String address, BigInteger minWeiAmount) throws Exception{
		return hasWeis(address, minWeiAmount, false);
	}

	private BigInteger hasWeis(String address, BigInteger minWeiAmount, boolean sysout) throws Exception{
		EthGetBalance balanceResponse = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
		BigInteger balance = balanceResponse.getBalance();

		if(sysout) {
			System.out.println(String.format("balance: %d account: %s" , balance, address));
		}

		Assert.assertTrue(String.format("not enough weis, expected at least %d, available %d for address %s", minWeiAmount, balance, address), balance.compareTo(minWeiAmount) >= 0);

		return balance;
	}

	/**
	 * check accounts available on the local client (need to have more than one)
	 */
	@Test
	public void verifyAccounts() throws Exception {
		if(setupFailed) {
			return;
		}

		EthAccounts accountsResponse = web3.ethAccounts().sendAsync().get();
		List<String> accounts = accountsResponse.getAccounts();

		Assert.assertTrue("less than 2 accounts available", accounts.size() > 1);

		int i = 0;
		for(String account: accounts) {
			isValidAddress(account, String.format("account [%d]", i++));
		}
	}

	/**
	 * check that moving ethers between accounts leads to proper balance
	 */
	@Test
	public void verifyTransferFunds() throws Exception {
		if(setupFailed) {
			return;
		}

		String account0 = getAccount(0); // coinbase
		String account1 = getAccount(1);

		BigInteger transferAmount = new BigInteger("1234567890"); 

		BigInteger balance0_before = getBalance(account0);
		BigInteger balance1_before = getBalance(account1);

		String txHash = transferEther(account0, account1, transferAmount);		

		BigInteger balance0_after = getBalance(account0);
		BigInteger balance1_after = getBalance(account1);

		// check that target address balance increased by exactly the transfer amount
		Assert.assertEquals("fund transfer incomplete for target account", balance1_before.add(transferAmount), balance1_after);

		EthTransaction ethTx = web3.ethGetTransactionByHash(txHash).sendAsync().get();
		org.web3j.protocol.core.methods.response.Transaction tx = ethTx.getTransaction().get();

		// check that source address balance decreased max by transfer amount plus total fees
		BigInteger gasPrice = tx.getGasPrice();
		BigInteger totalFees = tx.getGas().multiply(gasPrice);
		BigInteger maxAmountAndFeesToSubtract = transferAmount.add(totalFees);
		Assert.assertTrue("bad balance after transfer of funds", balance0_after.compareTo(balance0_before.subtract(maxAmountAndFeesToSubtract)) >= 0);

	}

	/**
	 * verify a number of transaction attributes 
	 */
	@Test
	public void verifyTransactionAndBlockAttributes() throws Exception {
		if(setupFailed) {
			return;
		}

		String account0 = getAccount(0); // coinbase
		String account1 = getAccount(1);
		BigInteger transferAmount = new BigInteger("3141592653");

		String txHash = transferEther(account0, account1, transferAmount);

		// query for tx
		org.web3j.protocol.core.methods.response.Transaction tx = getTransaction(txHash);

		String blockHash = tx.getBlockHash();
		BigInteger blockNumber = tx.getBlockNumber();
		String creates = tx.getCreates(); // this is null, what is it ?
		String from = tx.getFrom();
		String to = tx.getTo();
		BigInteger amount = tx.getValue();

		// check tx attributes
		Assert.assertTrue("tx hash does not match input hash", txHash.equals(tx.getHash()));
		Assert.assertTrue("tx block index invalid", blockNumber == null || blockNumber.compareTo(new BigInteger("0")) >= 0);
		Assert.assertTrue("tx from account does not match input account", account0.equals(from));
		Assert.assertTrue("tx to account does not match input account", account1.equals(to));
		Assert.assertTrue("tx transfer amount does not match input amount", transferAmount.equals(amount));

		// query for block
		Block blockByHash = getBlock(blockHash);
		Block blockByNumber = getBlock(blockNumber);

		Assert.assertTrue("bad tx hash for block by number", blockByNumber.getHash().equals(blockHash));
		Assert.assertTrue("bad tx number for block by hash", blockByHash.getNumber().equals(blockNumber));
		Assert.assertTrue("query block by hash and number have different parent hashes", blockByHash.getParentHash().equals(blockByNumber.getParentHash()));
		Assert.assertTrue("query block by hash and number results in different blocks", blockByHash.equals(blockByNumber));

		// find original tx in block
		boolean found = false;
		for(TransactionResult<?> txResult: blockByHash.getTransactions()) {
			TransactionObject txObject = (TransactionObject) txResult;

			// verify tx attributes returned by block query
			if(txObject.getHash().equals(txHash)) {
				Assert.assertTrue("tx from block has bad from", txObject.getFrom().equals(account0));
				Assert.assertTrue("tx from block has bad to", txObject.getTo().equals(account1));
				Assert.assertTrue("tx from block has bad amount", txObject.getValue().equals(transferAmount));
				found = true;
				break;
			}
		}

		Assert.assertTrue("tx not found in blocks transaction list", found);
	}

	private org.web3j.protocol.core.methods.response.Transaction getTransaction(String txHash) throws Exception {
		EthTransaction ethTx = web3.ethGetTransactionByHash(txHash).sendAsync().get();
		org.web3j.protocol.core.methods.response.Transaction tx = ethTx.getTransaction().get();
		return tx;
	}

	private Block getBlock(String blockHash) throws Exception {
		EthBlock ethBlock = web3.ethGetBlockByHash(blockHash, true).sendAsync().get();

		Assert.assertNotNull(String.format("failed to get block for hash %s", blockHash), ethBlock.getBlock());

		System.out.println("got block for hash " + blockHash);

		return ethBlock.getBlock(); 
	}

	private Block getBlock(BigInteger blockNumber) throws Exception {
		DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNumber);
		EthBlock ethBlock = web3.ethGetBlockByNumber(blockParameter, true).sendAsync().get();

		Assert.assertNotNull(String.format("failed to get block for number %d", blockNumber), ethBlock.getBlock());
		System.out.println("got block for number " + blockNumber);

		return ethBlock.getBlock(); 
	}

	@AfterClass
	public static void tearDown() {
	}

	private void isValidAddress(String address, String accountName) {
		// example address 0xb2681c93335d27aca783cc7d8039d045c06b988c
		Assert.assertTrue(String.format("%s address is empty", accountName), address != null && address.length() > 0);
		Assert.assertTrue(String.format("%s address does not start with '0x'", accountName), address.startsWith("0x"));
		Assert.assertEquals(String.format("%s address length != 42", accountName), 42, address.length());
	}


	private String transferEther(String from, String to, BigInteger amount) throws Exception {
		BigInteger nonce = getNonce(from);

		Transaction transaction = new Transaction(from, nonce, GAS_PRICE_DEFAULT, GAS_LIMIT_DEFAULT, to, amount, null);
		EthSendTransaction txRequest = web3.ethSendTransaction(transaction).sendAsync().get();
		String txHash = txRequest.getTransactionHash(); 

		Assert.assertTrue(String.format("tx has error state %s",  txRequest.getError()), !txRequest.hasError());
		Assert.assertTrue("tx hash is empty or null", txHash != null && txHash.startsWith(HEX_PREFIX));

		return txHash;
	}

	private BigInteger getNonce(String address) throws Exception {
		EthGetTransactionCount txCount = web3.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
		BigInteger nonce = txCount.getTransactionCount();

		Assert.assertTrue(String.format("nonce null for account %s", address), nonce != null);
		Assert.assertTrue(String.format("nonce is negative %d account %s", nonce, address), nonce.compareTo(new BigInteger("0")) >= 0);

		return nonce;
	}

	/**
	 * hack to compile solditiy source code (needed as long testrpc and web3j do not play along nicely
	 * see https://github.com/web3j/web3j/issues/53
	 */
	private JsonObject compileSolidityCode(String source) throws Exception {
		String compileCommandTemplate = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_compileSolidity\",\"params\":[\"%s\"],\"id\":1}";
		String compileCommand = String.format(compileCommandTemplate, normalizeSoliditySource(source));

		System.out.println(compileCommand);

		StringEntity requestEntity = new StringEntity(compileCommand, ContentType.create("text/plain").withCharset(Charset.forName("UTF-8")));
		HttpUriRequest request = RequestBuilder.post(String.format("http://%s:%s", CLIENT_IP, CLIENT_PORT)).setEntity(requestEntity).build();

		ResponseHandler<JsonObject> rh = new ResponseHandler<JsonObject>() {

			@Override
			public JsonObject handleResponse(final HttpResponse response) throws IOException {
				StatusLine statusLine = response.getStatusLine();
				HttpEntity entity = response.getEntity();

				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(
							statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				if (entity == null) {
					throw new ClientProtocolException("Response contains no content");
				}

				Gson gson = new GsonBuilder().create();
				Reader reader = new InputStreamReader(entity.getContent(), Charset.forName("UTF-8"));
				return gson.fromJson(reader, JsonObject.class);
			}
		};

		JsonObject response = HttpClients.createDefault().execute(request, rh);
		JsonObject result = response.get("result").getAsJsonObject();

		return result;
	}

	/**
	 * solidity source code needs to be reformatted to fit into a string variable (no line breaks allowed!)
	 */
	private String normalizeSoliditySource(String source) {
		return source == null ? null : source.replaceAll("\\s+", " ");
	}

	private BigInteger getBalance(String address) throws Exception {
		return hasWeis(address, new BigInteger("0"));
	}

	private BigInteger getBalance(String address, boolean sysout) throws Exception {
		return hasWeis(address, new BigInteger("0"), sysout);
	}

	private String getAccount(int i) throws Exception {
		EthAccounts accountsResponse = web3.ethAccounts().sendAsync().get();
		List<String> accounts = accountsResponse.getAccounts();

		return accounts.get(i);
	}

	// copied from org.web3j.protocol.scenarios.Scenario
	private TransactionReceipt waitForTransactionReceipt(
			String transactionHash) throws Exception {

		Optional<TransactionReceipt> transactionReceiptOptional =
				getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);

		if (!transactionReceiptOptional.isPresent()) {
			Assert.fail("Transaction receipt not generated after " + ATTEMPTS + " attempts");
		}

		return transactionReceiptOptional.get();
	}

	// copied from org.web3j.protocol.scenarios.Scenario
	private Optional<TransactionReceipt> getTransactionReceipt(
			String transactionHash, int sleepDuration, int attempts) throws Exception {

		Optional<TransactionReceipt> receiptOptional =
				sendTransactionReceiptRequest(transactionHash);
		for (int i = 0; i < attempts; i++) {
			if (!receiptOptional.isPresent()) {
				Thread.sleep(sleepDuration);
				receiptOptional = sendTransactionReceiptRequest(transactionHash);
			} else {
				break;
			}
		}

		return receiptOptional;
	}

	// copied from org.web3j.protocol.scenarios.Scenario
	private Optional<TransactionReceipt> sendTransactionReceiptRequest(
			String transactionHash) throws Exception {
		EthGetTransactionReceipt transactionReceipt =
				web3.ethGetTransactionReceipt(transactionHash).sendAsync().get();

		return transactionReceipt.getTransactionReceipt();
	}
}