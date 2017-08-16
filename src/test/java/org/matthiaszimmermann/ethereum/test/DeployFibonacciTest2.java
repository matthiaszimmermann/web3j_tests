package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.matthiaszimmermann.ethereum.test.Fibonacci.NotifyEventResponse;
import org.web3j.abi.datatypes.generated.Uint256;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;

public class DeployFibonacciTest2 extends EthereumTest {

	int countErrors = 0;
	int countEvents = 0;

	boolean debugTx = true;
	boolean debugBlock = false;

	@Test
	public void testDeployFibonacci() throws Exception {

		// used for sanity checking
		Subscription txSubscription = web3j.transactionObservable().subscribe(tx -> {
			if(debugTx) {
				System.out.println("new tx. nonce: " + tx.getNonce() + " value: " + tx.getValue() + " from address: " + tx.getFrom() + " to address: " + tx.getTo() + " tx hash: " + tx.getBlockHash() + " gas: " + tx.getGas());
				try {
					System.out.println("alice balance: " + getBalance(Alice.ADDRESS));
				} 
				catch (Exception e) {
					// nop
				}
			}
		});

		Subscription blockSubscription = web3j.blockObservable(false).subscribe(block -> {
			if(debugBlock) {
				System.out.println("new block. tx count: " + block.getBlock().getTransactions().size() + " block hash: " + block.getBlock().getHash());
			}
		});

		Fibonacci contract = deploy();

		// get observable for subscriptions
		Observable<NotifyEventResponse> obs = contract.notifyEventObservable();	

		// this would be the normal (run-mode, not unit-testing) case
		// Subscription subscription = addSubscription(obs);

		// testing observable -> http://fedepaol.github.io/blog/2015/09/13/testing-rxjava-observables-subscriptions/
		// TestSubscriber<NotifyEventResponse> subs = new TestSubscriber<>();
		EventSubscriber subs = new EventSubscriber();
		obs.subscribe(subs);

		contract.fibonacciNotify(new Uint256(BigInteger.valueOf(7)));
		contract.fibonacciNotify(new Uint256(BigInteger.valueOf(6)));
		contract.fibonacciNotify(new Uint256(BigInteger.valueOf(5)));
		
		verifyCalls(subs, 3);
		
		Thread.sleep(60000);
		
		// remove subscription
		subs.unsubscribe();
		txSubscription.unsubscribe();
		blockSubscription.unsubscribe();

		kill(contract);
	}

	private void verifyCalls(TestSubscriber<NotifyEventResponse> subscription, int callsExpected) {
		// provide max 10 seconds to complete
		System.out.println("waiting for n event notifications. n=" + callsExpected);
		subscription.awaitValueCount(callsExpected, 30, TimeUnit.SECONDS);

		// check for errors
		subscription.assertNoErrors();

		// check individual results
		List<NotifyEventResponse> events = subscription.getOnNextEvents();
		for(NotifyEventResponse e: events) {
			System.out.println("input: " + e.input.getValue() + " output: " + e.result.getValue());

			BigInteger n = e.input.getValue();
			Assert.assertEquals("Wrong output for fibonacci(n), n=" + n, fibonacci(n.intValue()), e.result.getValue().intValue());
		}

		// check number of results
		Assert.assertEquals("Wrong number of events", callsExpected, events.size());
	}

	private void verifyCalls(EventSubscriber subscription, int callsExpected) {
		// provide max 10 seconds to complete
		System.out.println("waiting for n event notifications. n=" + callsExpected);
		subscription.awaitEvents(callsExpected, 30, TimeUnit.SECONDS);

		// check for errors
		Assert.assertEquals("Bad number of errors", 0, subscription.getErrors().size());

		// check individual results
		List<NotifyEventResponse> events = subscription.getEvents();
		for(NotifyEventResponse e: events) {
			System.out.println("verify input: " + e.input.getValue() + " output: " + e.result.getValue());

			BigInteger n = e.input.getValue();
			Assert.assertEquals("Wrong output for fibonacci(n), n=" + n, fibonacci(n.intValue()), e.result.getValue().intValue());
		}

		// check number of results
		Assert.assertEquals("Wrong number of events", callsExpected, events.size());
	}

	private Subscription addSubscription(Observable<NotifyEventResponse> obs) {
		Subscription eventSubscription = obs.subscribe(emitter -> handleNormalCase(emitter), error -> handleErrorCase(error));
		return eventSubscription;
	}

	private Object handleNormalCase(NotifyEventResponse emitter) {
		System.out.println("Event notification. input: " + emitter.input.getValue() + " result: " + emitter.result.getValue());
		countEvents++;
		return null;
	}

	private Object handleErrorCase(Throwable error) {
		System.out.println("Event notification error: " + error);
		countErrors++;
		return null;
	}

	private Fibonacci deploy() throws Exception {

		// make sure alice has sufficient funds for deploy of contract
		BigInteger gasLimit = BigInteger.valueOf(400_000);
		BigInteger deployFunding = GAS_PRICE.multiply(gasLimit);
		ensureFundsForTransaction(Alice.ADDRESS, deployFunding);

		// deploy the contract with the owner's credentials
		BigInteger contractFunding = BigInteger.valueOf(0); 
		Fibonacci contract = Fibonacci.deploy(web3j, Alice.CREDENTIALS, GAS_PRICE, gasLimit, contractFunding).get();

		System.out.println("Contract deployed at " + contract.getContractAddress());

		return contract;
	}

	private void kill(Fibonacci contract) throws Exception {
		System.out.println("Fibonnaci contract does not have kill method");
	}

	private int fibonacci(int n) {
		// no argument checking here !!!
		if (n <= 1) return n;
		else return fibonacci(n-1) + fibonacci(n-2);
	}
	
	class EventSubscriber extends Subscriber<NotifyEventResponse> {

		private List<NotifyEventResponse> events = new ArrayList<>();
		private List<Throwable> errors = new ArrayList<>();
		
		public List<NotifyEventResponse> getEvents() {
			return events;
		}
		
		public List<Throwable> getErrors() {
			return errors;
		}
		
		public EventSubscriber() {
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
		public void onNext(NotifyEventResponse e) {
			System.out.println("EventSubscriber input: " + e.input.getValue() + " output: " + e.result.getValue());
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
