package org.matthiaszimmermann.ethereum.test;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

public class TransferEtherTest extends EthereumTest {
	
    @Test
    public void testEthSendTransaction() throws Exception {

        String from = getCoinbase();
        BigInteger nonce = getNonce(from);
        BigInteger amount = Convert.toWei("0.123", Convert.Unit.ETHER).toBigInteger();
        String to = Alice.ADDRESS;

        BigInteger toBalanceBefore = getBalance(to);
        
        // http://ethereum.stackexchange.com/questions/1832/cant-send-transaction-exceeds-block-gas-limit-or-intrinsic-gas-too-low
        BigInteger normalTxGasLimit = BigInteger.valueOf(21_000);

        // this is the method to test here
Transaction transaction = Transaction.createEtherTransaction(
                from, nonce, GAS_PRICE, normalTxGasLimit, to, amount);

        EthSendTransaction ethSendTx = web3j.ethSendTransaction(transaction).sendAsync().get();
        String txHash = ethSendTx.getTransactionHash();

        Assert.assertFalse(txHash.isEmpty());

        TransactionReceipt txReceipt = waitForTransactionReceipt(txHash);

        Assert.assertEquals(txReceipt.getTransactionHash(), txHash);
        Assert.assertEquals("Unexected balance for 'to' address", toBalanceBefore.add(amount), getBalance(to));
    }
	
    @Test
    public void testSendFunds() throws Exception {
    	BigDecimal etherAmount = BigDecimal.valueOf(0.321);
    	BigInteger weiAmount = Convert.toWei(etherAmount, Convert.Unit.ETHER).toBigInteger();
    	
    	ensureFundsForTransaction(Alice.ADDRESS, weiAmount);
    	
        BigInteger fromBalanceBefore = getBalance(Alice.ADDRESS);
        BigInteger toBalanceBefore = getBalance(Bob.ADDRESS);
        
        // this is the method to test here
        TransactionReceipt txReceipt = Transfer.sendFunds(
                web3j, Alice.CREDENTIALS, Bob.ADDRESS, etherAmount, Convert.Unit.ETHER);
        
        BigInteger txFees = txReceipt.getGasUsed().multiply(GAS_PRICE);
        
        Assert.assertFalse(txReceipt.getBlockHash().isEmpty());
        Assert.assertEquals("Unexected balance for 'from' address", fromBalanceBefore.subtract(weiAmount.add(txFees)), getBalance(Alice.ADDRESS));
        Assert.assertEquals("Unexected balance for 'to' address", toBalanceBefore.add(weiAmount), getBalance(Bob.ADDRESS));
    }

	@Test
	public void verifyTransactionAndBlockAttributes() throws Exception {
		String account0 = getCoinbase();
		String account1 = getAccount(1);
		BigInteger transferAmount = new BigInteger("3141592653");

		String txHash = transferEther(account0, account1, transferAmount);
		waitForTransactionReceipt(txHash);

		// query for tx
		org.web3j.protocol.core.methods.response.Transaction tx = getTransaction(txHash);

		String blockHash = tx.getBlockHash();
		BigInteger blockNumber = tx.getBlockNumber();
		String from = tx.getFrom();
		String to = tx.getTo();
		BigInteger amount = tx.getValue();

		// check tx attributes
		Assert.assertTrue("Tx hash does not match input hash", txHash.equals(tx.getHash()));
		Assert.assertTrue("Tx block index invalid", blockNumber == null || blockNumber.compareTo(new BigInteger("0")) >= 0);
		Assert.assertTrue("Tx from account does not match input account", account0.equals(from));
		Assert.assertTrue("Tx to account does not match input account", account1.equals(to));
		Assert.assertTrue("Tx transfer amount does not match input amount", transferAmount.equals(amount));

		// query for block
		Block blockByHash = getBlock(blockHash);
		Block blockByNumber = getBlock(blockNumber);

		Assert.assertTrue("Bad tx hash for block by number", blockByNumber.getHash().equals(blockHash));
		Assert.assertTrue("Bad tx number for block by hash", blockByHash.getNumber().equals(blockNumber));
		Assert.assertTrue("Query block by hash and number have different parent hashes", blockByHash.getParentHash().equals(blockByNumber.getParentHash()));
		Assert.assertTrue("Query block by hash and number results in different blocks", blockByHash.equals(blockByNumber));

		// find original tx in block
		boolean found = false;
		for(TransactionResult<?> txResult: blockByHash.getTransactions()) {
			TransactionObject txObject = (TransactionObject) txResult;

			// verify tx attributes returned by block query
			if(txObject.getHash().equals(txHash)) {
				Assert.assertTrue("Tx from block has bad from", txObject.getFrom().equals(account0));
				Assert.assertTrue("Tx from block has bad to", txObject.getTo().equals(account1));
				Assert.assertTrue("Tx from block has bad amount", txObject.getValue().equals(transferAmount));
				found = true;
				break;
			}
		}

		Assert.assertTrue("Tx not found in blocks transaction list", found);
	}

	private org.web3j.protocol.core.methods.response.Transaction getTransaction(String txHash) throws Exception {
		EthTransaction ethTx = web3j.ethGetTransactionByHash(txHash).sendAsync().get();
		org.web3j.protocol.core.methods.response.Transaction tx = ethTx.getTransaction().get();
		return tx;
	}

	private Block getBlock(String blockHash) throws Exception {
		EthBlock ethBlock = web3j.ethGetBlockByHash(blockHash, true).sendAsync().get();

		Assert.assertNotNull(String.format("Failed to get block for hash %s", blockHash), ethBlock.getBlock());

		System.out.println("Got block for hash " + blockHash);

		return ethBlock.getBlock(); 
	}

	private Block getBlock(BigInteger blockNumber) throws Exception {
		DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNumber);
		EthBlock ethBlock = web3j.ethGetBlockByNumber(blockParameter, true).sendAsync().get();

		Assert.assertNotNull(String.format("Failed to get block for number %d", blockNumber), ethBlock.getBlock());
		System.out.println("Got block for number " + blockNumber);

		return ethBlock.getBlock(); 
	}
	
}
