package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.Transaction;

import rx.Subscription;

public class TransactionFilterTest extends EthereumTest {

	int notifications = 0;
	int observedNumberOfBlocks = 0;

	// taken from https://community.oracle.com/docs/DOC-1011373
	@Test
	public void testBlockObservable() throws Exception {
		int sleepForPeriodOfSeconds = 30;
		int expectMinNumberOfBlocks = 10;
		
		observedNumberOfBlocks= 0;
		
		Subscription subscription = web3j.blockObservable(false).subscribe(eblock -> {
					Block block = eblock.getBlock();
					BigInteger blockNumber = block.getNumber();
					String blockHash = block.getHash();
					LocalDateTime timestamp = Instant.ofEpochSecond(block.getTimestamp().longValueExact())
							.atZone(ZoneId.of("UTC"))
							.toLocalDateTime();
					
					observedNumberOfBlocks++;
					System.out.println("Block event " + observedNumberOfBlocks + ". Block#: " + blockNumber + " timestamp: " + timestamp + " block Hash: " + blockHash);
		    	}, 
				Throwable::printStackTrace);

		TimeUnit.SECONDS.sleep(sleepForPeriodOfSeconds);
		subscription.unsubscribe();
		
		Assert.assertTrue("Not enough block events observed", observedNumberOfBlocks >= expectMinNumberOfBlocks);
	}

	@Test
	public void testTxObservable() throws Exception {

		String from = getAccount(0);
		String to = getAccount(1);
		BigInteger amount = BigInteger.valueOf(987654321);

		notifications = 0;

		Subscription pTxSubscription = web3j.pendingTransactionObservable().subscribe(ptx -> verifyTx("Pending", ptx, from, to, amount));
		Subscription txSubscription = web3j.transactionObservable().subscribe(tx -> verifyTx("Confirmed", tx, from, to, amount));

		String txHash = transferEther(from, to, amount);
		waitForTransactionReceipt(txHash);
		Thread.sleep(1000);

		Assert.assertEquals("Bad number of notifications", 2, notifications);

		try {
			txSubscription.unsubscribe();
			pTxSubscription.unsubscribe();
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}

	private void verifyTx(String prefix, Transaction tx, String from, String to, BigInteger amount) {
		System.out.println(prefix + " tx " + tx.getHash() + " amount " + tx.getValue());
		notifications++;
	}
}
