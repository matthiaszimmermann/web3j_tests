package org.matthiaszimmermann.ethereum.test;

import java.lang.String;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

/**
 * <p>Auto generated code.<br>
 * <strong>Do not modify!</strong><br>
 * Please use {@link org.web3j.codegen.SolidityFunctionWrapperGenerator} to update.
 *
 * <p>Generated with web3j version 2.0.0.
 */
public final class Greeter extends Contract {
    private static final String BINARY = "6060604052341561000c57fe5b6040516103b73803806103b7833981016040528080518201919050505b806001908051906020019061003f929190610088565b5033600060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505b5061012d565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106100c957805160ff19168380011785556100f7565b828001600101855582156100f7579182015b828111156100f65782518255916020019190600101906100db565b5b5090506101049190610108565b5090565b61012a91905b8082111561012657600081600090555060010161010e565b5090565b90565b61027b8061013c6000396000f3006060604052361561004a576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806341c0e1b514610053578063cfae321714610065575b6100515b5b565b005b341561005b57fe5b6100636100fe565b005b341561006d57fe5b610075610192565b60405180806020018281038252838181518152602001915080519060200190808383600083146100c4575b8051825260208311156100c4576020820191506020810190506020830392506100a0565b505050905090810190601f1680156100f05780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b600060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141561018f57600060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16ff5b5b565b61019a61023b565b60018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156102305780601f1061020557610100808354040283529160200191610230565b820191906000526020600020905b81548152906001019060200180831161021357829003601f168201915b505050505090505b90565b6020604051908101604052806000815250905600a165627a7a72305820488c3d711a840368f09f2ae5816fae5432a38f76aa7eb8e21de4d903da0d2db70029";

    private Greeter(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    private Greeter(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public Future<TransactionReceipt> kill() {
        Function function = new Function("kill", Arrays.<Type>asList(), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<Utf8String> greet() {
        Function function = new Function("greet", 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallSingleValueReturnAsync(function);
    }

    public static Future<Greeter> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialValue, Utf8String _greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_greeting));
        return deployAsync(Greeter.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor, initialValue);
    }

    public static Future<Greeter> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialValue, Utf8String _greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_greeting));
        return deployAsync(Greeter.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor, initialValue);
    }

    public static Greeter load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Greeter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Greeter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }
}
