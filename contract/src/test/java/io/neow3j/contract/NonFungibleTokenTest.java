package io.neow3j.contract;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.WireMockTestHelper.setUpWireMockForInvokeFunction;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.neow3j.TestProperties;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.protocol.core.response.NFTokenState;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.script.ScriptBuilder;
import io.neow3j.transaction.TransactionBuilder;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NonFungibleTokenTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private Account account1;
    private Account account2;
    private static final Hash160 NF_TOKEN_SCRIPT_HASH =
            Hash160.fromAddress("NQyYa8wycZRkEvQKr5qRUvMUwyDgvQMqL7");
    private static final byte[] TOKEN_ID = new byte[]{1, 2, 3};
    private static final String TRANSFER = "transfer";
    private static NonFungibleToken nfTestToken;

    @Before
    public void setUp() {
        // Configuring WireMock to use default host and the dynamic port set in WireMockRule.
        int port = this.wireMockRule.port();
        WireMock.configureFor(port);

        Neow3j neow = Neow3j.build(new HttpService("http://127.0.0.1:" + port));
        nfTestToken = new NonFungibleToken(NF_TOKEN_SCRIPT_HASH, neow);

        account1 = Account.fromWIF(TestProperties.defaultAccountWIF());
        account2 = Account.fromWIF(TestProperties.client1AccountWIF());
    }

    @Test
    public void testTransfer() throws IOException {
        setUpWireMockForInvokeFunction("decimals", "nft_decimals_0.json");
        setUpWireMockForInvokeFunction("ownerOf", "nft_ownerof.json");

        byte[] expectedScript = new ScriptBuilder()
                .contractCall(NF_TOKEN_SCRIPT_HASH, TRANSFER,
                        asList(
                                hash160(account2.getScriptHash()),
                                byteArray(TOKEN_ID),
                                null))
                .toArray();

        Wallet wallet = Wallet.withAccounts(account1);
        TransactionBuilder b = nfTestToken.transfer(wallet, account2.getScriptHash(), TOKEN_ID);
        assertThat(b.getScript(), is(expectedScript));
    }

    @Test
    public void testTransfer_WalletDoesNotContainTokenOwner() throws IOException {
        setUpWireMockForInvokeFunction("decimals", "nft_decimals_0.json");
        setUpWireMockForInvokeFunction("ownerOf", "nft_ownerof.json");

        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The provided wallet does not contain the account");
        nfTestToken.transfer(Wallet.withAccounts(account2), account1.getScriptHash(), TOKEN_ID);
    }

    @Test
    public void testOwnerOf() throws IOException {
        setUpWireMockForInvokeFunction("ownerOf", "nft_ownerof.json");
        Hash160 owner = nfTestToken.ownerOf(TOKEN_ID);

        assertThat(owner, is(account1.getScriptHash()));
    }

    @Test
    public void testOwnerOf_returnNotScriptHash() throws IOException {
        setUpWireMockForInvokeFunction("ownerOf", "response_stack_integer.json");
        exceptionRule.expect(UnexpectedReturnTypeException.class);
        exceptionRule.expectMessage("but expected ByteString");
        nfTestToken.ownerOf(new byte[]{1});
    }

    @Test
    public void testOwnerOf_returnInvalidAddress() throws IOException {
        setUpWireMockForInvokeFunction("ownerOf", "response_invalid_address.json");
        exceptionRule.expect(UnexpectedReturnTypeException.class);
        exceptionRule.expectMessage("Return type did not contain script hash in expected format.");
        nfTestToken.ownerOf(new byte[]{1});
    }

    @Test
    public void testGetDecimals() throws IOException {
        setUpWireMockForInvokeFunction("decimals", "nft_decimals_5.json");
        assertThat(nfTestToken.getDecimals(), is(5));
    }

    @Test
    public void testBalanceOf() throws IOException {
        setUpWireMockForInvokeFunction("balanceOf", "nft_balanceof.json");
        BigInteger balance = nfTestToken.balanceOf(account1.getScriptHash());

        assertThat(balance, is(new BigInteger("244")));
    }

    @Test
    public void testGetProperties() throws IOException {
        setUpWireMockForInvokeFunction("properties", "nft_properties.json");
        NFTokenState properties = nfTestToken.properties(new byte[]{1});

        assertThat(properties.getName(), is("A name"));
    }

    @Test
    public void testGetProperties_unexpectedReturnType() throws IOException {
        setUpWireMockForInvokeFunction("properties", "response_stack_integer.json");
        exceptionRule.expect(UnexpectedReturnTypeException.class);
        exceptionRule.expectMessage("but expected Map");
        nfTestToken.properties(new byte[]{1});
    }

}
