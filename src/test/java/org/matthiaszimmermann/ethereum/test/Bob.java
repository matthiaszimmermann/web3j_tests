package org.matthiaszimmermann.ethereum.test;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

public class Bob {
	private static final String PRIVATE_KEY = "0xc85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
	private static final ECKeyPair KEY_PAIR = ECKeyPair.create(Numeric.toBigInt(PRIVATE_KEY));

	public static final Credentials CREDENTIALS = Credentials.create(KEY_PAIR);
	public static final String ADDRESS = CREDENTIALS.getAddress();
}
