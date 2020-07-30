package io.neow3j.devpack.framework;

import io.neow3j.devpack.framework.annotations.Appcall;

/**
 * Provides convenience methods to call Neo-native contracts (NEO, GAS, and Policy)
 */
// TODO: Update script hashes if native contracts change.
public class Native {

    /**
     * Calls the NEO token contract with the given method and arguments.
     * <p>
     * It is up to the developer to cast the result to the correct type.
     *
     * @param method    The method to call.
     * @param arguments The arguments to hand to the method.
     * @return the result of the execution.
     */
    @Appcall(scriptHash = "0x9bde8f209c88dd0e7ca3bf0af0f476cdd8207789")
    public static native Object callNeoContract(String method, Object[] arguments);

    /**
     * Calls the GAS token contract with the given method and arguments.
     * <p>
     * It is up to the developer to cast the result to the correct type.
     *
     * @param method    The method to call.
     * @param arguments The arguments to hand to the method.
     * @return the result of the execution.
     */
    @Appcall(scriptHash = "0x8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b")
    public static native Object callGasContract(String method, Object... arguments);

    /**
     * Calls the policy contract with the given method and arguments.
     * <p>
     * It is up to the developer to cast the result to the correct type.
     *
     * @param method    The method to call.
     * @param arguments The arguments to hand to the method.
     * @return the result of the execution.
     */
    @Appcall(scriptHash = "0xce06595079cd69583126dbfd1d2e25cca74cffe9")
    public static native Object callPolicyContract(String method, Object... arguments);

}