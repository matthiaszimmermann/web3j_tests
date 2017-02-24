package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.protocol.core.methods.response.Transaction;

import rx.Subscription;

public class TransactionFilterTest extends EthereumTest {

	int notifications = 0;

	@Test
	public void testTransactionFilter() throws Exception {

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
