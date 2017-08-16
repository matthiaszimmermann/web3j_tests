package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.matthiaszimmermann.ethereum.test.Savings.DepositEventResponse;
import org.matthiaszimmermann.ethereum.test.Savings.WithdrawalEventResponse;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

public class DeploySavingsTest extends EthereumTest {

	Savings contract = null;

	// TODO overwrite setup + teardown for deploy and kill
	@Override
	public void setUp() throws Exception {
		super.setUp();

		Credentials credentials = Alice.CREDENTIALS;
		Uint256 limitWei = new Uint256(BigInteger.valueOf(1000));
		Uint256 periodSeconds = new Uint256(BigInteger.valueOf(2));

		// deploy contract
		contract = deploy(Alice.CREDENTIALS, limitWei, periodSeconds);

		EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, contract.getContractAddress());
		web3j.ethLogObservable(filter).subscribe(log -> { System.out.println("LogEvent " + log.getType()); });
	}

	@After
	public void tearDown() throws Exception {

		// kill contract
		kill2(Alice.CREDENTIALS, contract);
	}

	// TODO write separate test method for each contract method 

	@Test
	public void testDeploySavings() throws Exception {

		Credentials credentials = Alice.CREDENTIALS;

		// TODO try this:
		// https://docs.web3j.io/filters.html#topic-filters-and-evm-events
		// EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, <contract-address>);
		// web3j.ethLogObservable(filter).subscribe(log -> { sysout });

		// add deposit event subscriber
		Observable<DepositEventResponse> depositObservable = contract.depositEventObservable();
		DepositEventSubscriber depositSubscriber = new DepositEventSubscriber();
		depositObservable.subscribe(depositSubscriber);

		// add withdrawal event subscriber
		Observable<WithdrawalEventResponse> withdrawalObservable = contract.withdrawalEventObservable();
		WithdrawalEventSubscriber withdrawalSubscriber = new WithdrawalEventSubscriber();
		withdrawalObservable.subscribe(withdrawalSubscriber);

		// check test subscribtor
		TestSubscriber<DepositEventResponse> testSubscriber = new TestSubscriber<>();
		depositObservable.subscribe(testSubscriber);

		System.out.println("Contract owner balance (post-deploy): " + getBalance(credentials.getAddress()));

		// get contract address (after deploy)
		Assert.assertNotNull(contract);
		String contractAddress = contract.getContractAddress(); 
		Assert.assertTrue("Contract address does not start with '0x'", contractAddress.startsWith("0x"));
		System.out.println("Contract address balance (initial): " + getBalance(contractAddress));

		// TODO try to withdraw money from empty account

		// add money to savings contract
		BigInteger amount = BigInteger.valueOf(1000);
		transferFunds(contractAddress, amount);
		System.out.println("Contract address balance (after funding): " + getBalance(contractAddress));

		// withdraw some funds as alice
		Uint256 withdrawAmount = new Uint256(BigInteger.valueOf(200));
		Future<TransactionReceipt> withdrawResult = contract.withdraw(withdrawAmount);
		contract.withdraw(new Uint256(BigInteger.valueOf(100)));
		contract.withdraw(new Uint256(BigInteger.valueOf(0)));

		waitForEvents(withdrawalSubscriber, 3);
		verifyCalls(testSubscriber, 1);

		// TODO try to withdraw funds as bob
		// Savings.load(contractAddress, web3j, Bob.CREDENTIALS, GAS_PRICE, deployGasLimit);

		// remove subscription
		depositSubscriber.unsubscribe();
		withdrawalSubscriber.unsubscribe();

	}

	private Savings deploy(Credentials credentials, Uint256 limitWei, Uint256 timeout) throws Exception {

		// make sure account has sufficient funds for deploy of contract
		BigInteger gasLimit = BigInteger.valueOf(400_000);
		BigInteger deployFunding = GAS_PRICE.multiply(gasLimit);
		ensureFundsForTransaction(credentials.getAddress(), deployFunding);

		// deploy the contract with the owner's credentials
		BigInteger contractFunding = BigInteger.valueOf(0); 
		Savings contract = Savings.deploy(web3j, credentials, GAS_PRICE, gasLimit, contractFunding, limitWei, timeout).get();

		System.out.println("Contract deployed at " + contract.getContractAddress());

		return contract;
	}

	private void kill2(Credentials credentials, Savings contract) throws Exception {
		// kill contract
		String contractOwnerAdress = credentials.getAddress();
		String contractAddress = contract.getContractAddress();
		BigInteger ownerBalanceBeforeKill = getBalance(contractOwnerAdress);
		Future<TransactionReceipt> txReceiptFuture = contract.kill();
		TransactionReceipt txReceipt = txReceiptFuture.get();
		Assert.assertNotNull(txReceipt);
		BigInteger ownerBalanceAfterKill = getBalance(contractOwnerAdress);
		BigInteger killFees = txReceipt.getCumulativeGasUsed().multiply(GAS_PRICE);
		System.out.println("Gas used (cumulative): " + txReceipt.getCumulativeGasUsed() + " kill fees: " + killFees);

//		Assert.assertEquals("Bad contract owner balance after killing contract", ownerBalanceAfterKill, ownerBalanceBeforeKill.add(contractFundingAmount).subtract(killFees));
		System.out.println("Contract address balance (after kill): " + getBalance(contractAddress));
		System.out.println("Contract owner balance (after kill): " + getBalance(contractOwnerAdress));

		// try to run greet again (expect ExecutionException)
		exception.expect(ExecutionException.class);		
		Future<Uint256> limitFuture = contract.limit();
		try {
			limitFuture.get();
		}
		catch(Exception e) {
			System.out.println("Ok case: failed to call limit() on killed contract: " + e);
			throw e;
		}
	}
	
	private void kill(Credentials credentials, Savings contract) throws Exception {
		String contractOwnerAdress = credentials.getAddress();
		String contractAddress = contract.getContractAddress(); 
		BigInteger ownerBalanceBeforeKill = getBalance(contractOwnerAdress);
		System.out.println("Contract owner balance (before kill): " + ownerBalanceBeforeKill);

		// kill the contract
		TransactionReceipt txReceipt = contract.kill().get();

		Assert.assertNotNull(txReceipt);
		BigInteger ownerBalanceAfterKill = getBalance(contractOwnerAdress);
		BigInteger killFees = txReceipt.getCumulativeGasUsed().multiply(GAS_PRICE);
		System.out.println("Gas used (cumulative): " + txReceipt.getCumulativeGasUsed() + " kill fees: " + killFees);

		System.out.println("Contract address balance (after kill): " + getBalance(contractAddress));
		System.out.println("Contract owner balance (after kill): " + ownerBalanceAfterKill + " difference: " + ownerBalanceAfterKill.subtract(ownerBalanceBeforeKill));

		// try to run greet again (expect ExecutionException)
		exception.expect(ExecutionException.class);		
		Uint256 withdrawAmount = new Uint256(BigInteger.valueOf(1));
		Future<TransactionReceipt> withdrawTx = contract.withdraw(withdrawAmount);

		try {
			TransactionReceipt withdrawResult = withdrawTx.get();
		}
		catch(Exception e) {
			System.out.println("Ok case: failed to call withdraw() on killed contract: " + e);
			throw e;
		}
	}

	private void waitForEvents(WithdrawalEventSubscriber subscription, int callsExpected) {
		System.out.println("waiting for n event notifications. n=" + callsExpected);
		subscription.awaitEvents(callsExpected, 30, TimeUnit.SECONDS);

		// check for errors
		Assert.assertEquals("Bad number of errors", 0, subscription.getErrors().size());

		// check individual results
		List<WithdrawalEventResponse> events = subscription.getEvents();
		for(WithdrawalEventResponse e: events) {
			System.out.println("Withdrawal event  message: " + e.message.getTypeAsString() + " value: " + e.value.getValue());
		}

		// check number of results
		// Assert.assertEquals("Wrong number of events", callsExpected, events.size());
	}

	class WithdrawalEventSubscriber extends Subscriber<WithdrawalEventResponse> {

		private List<WithdrawalEventResponse> events = new ArrayList<>();
		private List<Throwable> errors = new ArrayList<>();

		public List<WithdrawalEventResponse> getEvents() {
			return events;
		}

		public List<Throwable> getErrors() {
			return errors;
		}

		public WithdrawalEventSubscriber() {
			super();
			System.out.println("EventSubscriber constructor called");
		}

		@Override
		public void onCompleted() {
			System.out.println("EventSubscriber.onCompleted()");
		}

		@Override
		public void onError(Throwable t) {
			System.out.println("throwable: " + t.getMessage());
			errors.add(t);
		}

		@Override
		public void onNext(WithdrawalEventResponse e) {
			System.out.println("EventSubscriber message: " + e.message.getTypeAsString() + " value: " + e.value.getValue());
			events.add(e);
		}

		public final boolean awaitEvents(int callsExpected, long timeout, TimeUnit unit) {
			int valueCount = events.size();

			while (timeout != 0 && valueCount < callsExpected) {
				try {
					unit.sleep(1);
				} catch (InterruptedException e) {
					throw new IllegalStateException("Interrupted", e);
				}
				timeout--;
			}
			return valueCount >= callsExpected;
		}
	}


	private void verifyCalls(TestSubscriber<DepositEventResponse> subscription, int callsExpected) {
		// provide max 10 seconds to complete
		System.out.println("waiting for n event notifications. n=" + callsExpected);
		subscription.awaitValueCount(callsExpected, 30, TimeUnit.SECONDS);

		// check for errors
		subscription.assertNoErrors();

		// check individual results
		List<DepositEventResponse> events = subscription.getOnNextEvents();
		for(DepositEventResponse e: events) {
			System.out.println("value: " + e.value.getTypeAsString() + " sender: " + e.from.getTypeAsString());
		}

		// check number of results
		Assert.assertEquals("Wrong number of events", callsExpected, events.size());
	}

	class DepositEventSubscriber extends Subscriber<DepositEventResponse> {

		private List<DepositEventResponse> events = new ArrayList<>();
		private List<Throwable> errors = new ArrayList<>();

		public List<DepositEventResponse> getEvents() {
			return events;
		}

		public List<Throwable> getErrors() {
			return errors;
		}

		public DepositEventSubscriber() {
			super();
			System.out.println("EventSubscriber constructor called");
		}

		@Override
		public void onCompleted() {
			System.out.println("EventSubscriber.onCompleted()");
		}

		@Override
		public void onError(Throwable t) {
			System.out.println("throwable: " + t.getMessage());
			errors.add(t);
		}

		@Override
		public void onNext(DepositEventResponse e) {
			System.out.println("EventSubscriber sender: " + e.from.getTypeAsString() + " value: " + e.value.getValue());
			events.add(e);
		}

		public final boolean awaitEvents(int callsExpected, long timeout, TimeUnit unit) {
			int valueCount = events.size();

			while (timeout != 0 && valueCount < callsExpected) {
				try {
					unit.sleep(1);
				} catch (InterruptedException e) {
					throw new IllegalStateException("Interrupted", e);
				}
				timeout--;
			}
			return valueCount >= callsExpected;
		}
	}

}
