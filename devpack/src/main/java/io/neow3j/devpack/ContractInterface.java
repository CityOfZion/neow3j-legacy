package io.neow3j.devpack;

import io.neow3j.devpack.annotations.ContractScriptHash;

/**
 * Base class for contract interfaces that give convenient access to a deployed contract's methods.
 * Extend this class in combination with the {@link ContractScriptHash} annotation to create an
 * "interface" to smart contract on the Neo blockchain. Examples are the {@link Policy} and {@link
 * NEO} contracts.
 */
public abstract class ContractInterface {

    /**
     * Gets the contract's script hash. This requires the extending class to use the {@link
     * ContractScriptHash} annotation.
     *
     * @return the contract's script hash.
     */
    public static native byte[] getHash();

}
