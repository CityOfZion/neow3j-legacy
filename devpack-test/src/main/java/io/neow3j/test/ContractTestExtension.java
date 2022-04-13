package io.neow3j.test;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.ContractManagement;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.script.VerificationScript;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static java.lang.String.format;

public class ContractTestExtension implements BeforeAllCallback, AfterAllCallback {

    // Extension Context Store
    final static String CHAIN_STORE_KEY = "testChain";
    final static String NEOW3J_STORE_KEY = "neow3j";
    final static String DEPLOY_CTX_STORE_KEY = "contracts";

    private Neow3j neow3j;
    private DeployContext deployCtx = new DeployContext();
    private TestBlockchain chain;

    public ContractTestExtension() {
        chain = new NeoExpressTestContainer();
    }

    public ContractTestExtension(TestBlockchain chain) {
        this.chain = chain;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ContractTest annotation = context.getTestClass().get().getAnnotation(ContractTest.class);
        if (annotation == null) {
            throw new ExtensionConfigurationException("Using the " + this.getClass().getSimpleName()
                    + " without the @" + ContractTest.class.getSimpleName() + " annotation.");
        }
        if (annotation.blockTime() != 0) {
            chain.withSecondsPerBlock(annotation.blockTime());
        }
        if (!annotation.configFile().isEmpty()) {
            chain.withConfigFile(annotation.configFile());
        }
        if (!annotation.batchFile().isEmpty()) {
            chain.withBatchFile(annotation.batchFile());
        }
        if (!annotation.checkpoint().isEmpty()) {
            chain.withCheckpoint(annotation.checkpoint());
        }
        chain.start();
        neow3j = Neow3j.build(new HttpService(chain.getNodeUrl()));

        for (Class<?> c : annotation.contracts()) {
            Method m = findCorrespondingDeployConfigMethod(c, context);
            DeployConfiguration config = new DeployConfiguration();
            if (m != null) {
                if (m.getParameterCount() == 0) {
                    config = (DeployConfiguration) m.invoke(null);
                } else if (m.getParameterCount() == 1) {
                    config = (DeployConfiguration) m.invoke(null, deployCtx);
                }
            }
            try {
                Await.waitUntilTransactionIsExecuted(compileAndDeploy(c, config, neow3j), neow3j);
            } catch (Throwable t) {
                throw new Exception(t);
            }
        }

        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.GLOBAL);
        store.put(CHAIN_STORE_KEY, chain);
        store.put(NEOW3J_STORE_KEY, neow3j);
        store.put(DEPLOY_CTX_STORE_KEY, deployCtx);
    }

    private Method findCorrespondingDeployConfigMethod(Class<?> contract, ExtensionContext ctx) {
        List<Method> methods = Arrays.stream(ctx.getTestClass().get().getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) &&
                        m.isAnnotationPresent(DeployConfig.class) &&
                        m.getAnnotation(DeployConfig.class).value().equals(contract))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() > 1) {
            throw new ExtensionConfigurationException("Specified more than one deployment " +
                    "configuration method for contract class " + contract.getCanonicalName());
        }
        Method method = methods.get(0);
        if (!method.getReturnType().equals(DeployConfiguration.class)) {
            throw new ExtensionConfigurationException("Methods annotated with '" +
                    DeployConfig.class.getSimpleName() + "' must return 'DeployConfiguration'.");
        }

        if (method.getParameterCount() != 0
                && !method.getParameterTypes()[0].equals(DeployContext.class)) {
            throw new ExtensionConfigurationException(format("Methods annotated with '%s' must " +
                            "have either no parameter of an optional '%s' parameter.",
                    DeployConfig.class.getSimpleName(), DeployContext.class.getSimpleName()));
        }
        return method;
    }

    private Hash256 compileAndDeploy(Class<?> contractClass, DeployConfiguration conf,
            Neow3j neow3j) throws Throwable {

        CompilationUnit res;
        if (conf.getSubstitutions().isEmpty()) {
            res = new Compiler().compile(contractClass.getCanonicalName());
        } else {
            res = new Compiler().compile(contractClass.getCanonicalName(), conf.getSubstitutions());
        }

        AccountSigner signer = conf.getSigner();
        Account[] multiSigSigners = conf.getSigningAccounts();
        if (signer == null) {
            // If the user hasn't set a specific deploying account use the genesis account.
            TestBlockchain.GenesisAccount genAcc = chain.getGenesisAccount();
            signer = AccountSigner.none(Account.fromVerificationScript(new VerificationScript(
                    hexStringToByteArray(genAcc.getVerificationScript()))));
            multiSigSigners = Arrays.stream(genAcc.getPrivateKeys())
                    .map(k -> new Account(ECKeyPair.create(hexStringToByteArray(k))))
                    .toArray(Account[]::new);
        }
        TransactionBuilder builder = new ContractManagement(neow3j)
                .deploy(res.getNefFile(), res.getManifest(), conf.getDeployParam())
                .signers(signer);
        Transaction tx;
        if (signer.getAccount().isMultiSig()) {
            tx = builder.getUnsignedTransaction().addMultiSigWitness(
                    signer.getAccount().getVerificationScript(), multiSigSigners);
        } else {
            tx = builder.sign();
        }
        Hash256 txHash = tx.send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(txHash, neow3j);

        NeoApplicationLog log = neow3j.getApplicationLog(txHash).send().getApplicationLog();
        if (log.getExecutions().get(0).getState().equals(NeoVMStateType.FAULT)) {
            throw new ExtensionConfigurationException("Failed to deploy smart contract. NeoVM " +
                    "error message: " + log.getExecutions().get(0).getException());
        }
        Hash160 contractHash = SmartContract.calcContractHash(signer.getScriptHash(),
                res.getNefFile().getCheckSumAsInteger(), res.getManifest().getName());
        deployCtx.addDeployTxHash(contractClass, txHash);
        deployCtx.addDeployedContract(contractClass, new SmartContract(contractHash, neow3j));
        return txHash;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        chain.stop();
    }

    /**
     * Gets an instance of {@code SmartContract} for the given contract under test. It can be
     * used as a handle to interact with the contract.
     *
     * @param contractClass The contract to get the {@code SmartContract} instance for.
     * @return the {@code SmartContract} instance.
     */
    public SmartContract getDeployedContract(Class<?> contractClass) {
        return deployCtx.getDeployedContract(contractClass);
    }

    /**
     * Gets the hash of the transaction in which the given contract was deployed.
     *
     * @param contractClass The class of the deployed contract.
     * @return the transaction hash.
     */
    public Hash256 getDeployTxHash(Class<?> contractClass) {
        return deployCtx.getDeployTxHash(contractClass);
    }

    /**
     * Gets the Neow3j instance that allows for calls to the underlying blockchain instance.
     *
     * @return the Neow3j instance.
     */
    public Neow3j getNeow3j() {
        return neow3j;
    }

    /**
     * Resumes the blockchain if it was stopped before.
     *
     * @throws Exception if resuming the blockchain failed.
     */
    public void resume() throws Exception {
        chain.resume();
    }

    /**
     * Halts the blockchain, i.e., stops block production.
     *
     * @throws Exception if halting the blockchain failed.
     */
    public void halt() throws Exception {
        chain.halt();
    }

    /**
     * Creates a new account and returns it.
     *
     * @return The account.
     * @throws Exception if creating the account failed.
     */
    public Account createAccount() throws Exception {
        return new Account(ECKeyPair.create(
                hexStringToByteArray(chain.getAccount(chain.createAccount()))));
    }

    /**
     * Fast-forwards the blockchain state by {@code n} blocks. I.e., mints {@code n} empty blocks.
     * Can be used on a running or stopped node.
     *
     * @param n The number of blocks to mint.
     * @throws Exception if minting new blocks failed.
     */
    public void fastForward(int n) throws Exception {
        chain.fastForward(n);
    }

    /**
     * Gets the account for the given address if it exists on the blockchain.
     *
     * @param address The account's address.
     * @return The account.
     * @throws Exception if an error occurs when tyring to fetch the account from the
     *                   underlying blockchain/node.
     */
    public Account getAccount(String address) throws Exception {
        return new Account(ECKeyPair.create(hexStringToByteArray(
                chain.getAccount(address))));
    }

    /**
     * If the underlying test blockchain implementation has control over the genesis account, it
     * will be returned with all signer accounts.
     *
     * @return The genesis account's verification script and private keys.
     */
    public GenesisAccount getGenesisAccount() {
        try {
            TestBlockchain.GenesisAccount genAcc = chain.getGenesisAccount();
            Account multiSigAccount = Account.fromVerificationScript(
                    new VerificationScript(hexStringToByteArray(genAcc.getVerificationScript())));
            Account[] signerAccounts = Arrays.stream(genAcc.getPrivateKeys())
                    .map(k -> new Account(ECKeyPair.create(hexStringToByteArray(k))))
                    .toArray(Account[]::new);
            return new GenesisAccount(multiSigAccount, signerAccounts);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Contains all necessary information to make transactions with the genesis account of a
     * blockchain. The genesis account is the account that holds all native assets at the
     * beginning of a new chain.
     */
    public static class GenesisAccount {

        private Account multiSigAccount;
        private Account[] signerAccounts;

        public GenesisAccount(Account multiSigAccount, Account[] signerAccounts) {
            this.multiSigAccount = multiSigAccount;
            this.signerAccounts = signerAccounts;
        }

        /**
         * Gets the genesis account, i.e., the multi-sig account that is the genesis account.
         *
         * @return the genesis account.
         */
        public Account getMultiSigAccount() {
            return multiSigAccount;
        }

        /**
         * Gets the accounts, including their private keys, that are part of the genesis
         * multi-sig account.
         *
         * @return the participating accounts.
         */
        public Account[] getSignerAccounts() {
            return signerAccounts;
        }
    }

}