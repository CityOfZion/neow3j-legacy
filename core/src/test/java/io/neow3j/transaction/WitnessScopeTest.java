package io.neow3j.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import io.neow3j.protocol.ObjectMapperFactory;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WitnessScopeTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void combineScopes() {
        byte scope = WitnessScope.combineScopes(Arrays.asList(
                WitnessScope.CALLED_BY_ENTRY,
                WitnessScope.CUSTOM_CONTRACTS));
        assertThat(scope, is((byte) 0x11));

        scope = WitnessScope.combineScopes(Arrays.asList(
                WitnessScope.CALLED_BY_ENTRY,
                WitnessScope.CUSTOM_CONTRACTS,
                WitnessScope.CUSTOM_GROUPS));
        assertThat(scope, is((byte) 0x31));

        scope = WitnessScope.combineScopes(Arrays.asList(WitnessScope.GLOBAL));
        assertThat(scope, is((byte) 0x80));

        scope = WitnessScope.combineScopes(Arrays.asList(WitnessScope.NONE));
        assertThat(scope, is((byte) 0x00));
    }

    @Test
    public void extractCombinedScopes() {
        List<WitnessScope> scopes = WitnessScope.extractCombinedScopes((byte) 0x00);
        assertThat(scopes, contains(WitnessScope.NONE));

        scopes = WitnessScope.extractCombinedScopes((byte) 0x80);
        assertThat(scopes, contains(WitnessScope.GLOBAL));

        scopes = WitnessScope.extractCombinedScopes((byte) 0x11);
        assertThat(scopes, containsInAnyOrder(
                WitnessScope.CALLED_BY_ENTRY,
                WitnessScope.CUSTOM_CONTRACTS));

        scopes = WitnessScope.extractCombinedScopes((byte) 0x21);
        assertThat(scopes, containsInAnyOrder(
                WitnessScope.CALLED_BY_ENTRY,
                WitnessScope.CUSTOM_GROUPS));

        scopes = WitnessScope.extractCombinedScopes((byte) 0x31);
        assertThat(scopes, containsInAnyOrder(
                WitnessScope.CALLED_BY_ENTRY,
                WitnessScope.CUSTOM_GROUPS,
                WitnessScope.CUSTOM_CONTRACTS));

    }

    @Test
    public void fromJson() throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
        String json = "\"None\"";
        WitnessScope scope = mapper.readValue(json, WitnessScope.class);
        assertThat(scope, is(WitnessScope.NONE));

        json = "1";
        scope = mapper.readValue(json, WitnessScope.class);
        assertThat(scope, is(WitnessScope.CALLED_BY_ENTRY));
    }

    @Test
    public void failFromJson() throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
        String json = "\"NonExistent\"";

        expectedException.expect(InvalidDefinitionException.class);
        expectedException.expectMessage(new StringContains(
                String.format("%s value type not found.", WitnessScope.class.getName())));
        WitnessScope scope = mapper.readValue(json, WitnessScope.class);
    }

}