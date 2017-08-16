package org.matthiaszimmermann.ethereum.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.parity.methods.response.PersonalUnlockAccount;

public class Web3jExt extends JsonRpc2_0Web3j {

	public Web3jExt(Web3jService web3jService) {
		super(web3jService);
	}

	public Web3jExt(JsonRpc2_0Web3j jsonWeb3j) {
		super(((Web3jExt)jsonWeb3j).web3jService);
	}
	
	Web3jService getService() {
		return web3jService;
	}
	
	/**
	 * method copied from parity
	 */
    public Request<?, PersonalUnlockAccount> personalUnlockAccount(
            String accountId, String password, BigInteger duration) {
        List<Object> attributes = new ArrayList<>(3);
        attributes.add(accountId);
        attributes.add(password);

        if (duration != null) {
            // Parity has a bug where it won't support a duration
            // See https://github.com/ethcore/parity/issues/1215
            attributes.add(duration.longValue());
        } else {
            attributes.add(null);  // we still need to include the null value, otherwise Parity rejects
        }

        return new Request<>(
                "personal_unlockAccount",
                attributes,
                ID,
                web3jService,
                PersonalUnlockAccount.class);
    }
}
