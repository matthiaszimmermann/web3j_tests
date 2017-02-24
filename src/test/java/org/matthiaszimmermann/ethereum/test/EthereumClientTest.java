package org.matthiaszimmermann.ethereum.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

public class EthereumClientTest  extends EthereumTest {
	
	@Test
	public void testClientVersion() throws Exception {
		Web3ClientVersion versionResponse = web3j.web3ClientVersion().sendAsync().get();
		String version = versionResponse.getWeb3ClientVersion();

		Assert.assertTrue(String.format("Client version '%s' doesn't contain TestRPC or Geth",  version), version.contains("TestRPC") || version.contains("Geth"));
		
		System.out.println("Client version: " + version);
	}

	@Test
	public void testCoinbase() throws Exception {
		EthCoinbase coinbase = web3j.ethCoinbase().sendAsync().get();
		String address = coinbase.getAddress();

		Assert.assertTrue("Coinbase address does not start with '0x'", address.startsWith("0x"));
		Assert.assertEquals("Wrong coinbase address", getCoinbase(), address);
		
		System.out.println(String.format("Coinbase address %s balance %d", address, getBalance(address)));
	}
	
	@Test
	public void testAccounts() throws Exception {
		EthAccounts accountsResponse = web3j.ethAccounts().sendAsync().get();
		List<String> accounts = accountsResponse.getAccounts();

		Assert.assertTrue("Less than 2 accounts available", accounts.size() > 1);

		int i = 0;
		for(String account: accounts) {
			System.out.println(String.format("Account [%d]: %d", i++, getBalance(account)));
		}
	}
	
}
