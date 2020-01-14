package io.neow3j.contract;

import io.neow3j.constants.InteropServiceCode;
import io.neow3j.constants.NeoConstants;
import io.neow3j.io.BinaryReader;
import io.neow3j.io.BinaryWriter;
import io.neow3j.io.NeoSerializable;
import io.neow3j.io.exceptions.DeserializationException;
import java.io.IOException;
import java.math.BigDecimal;

public class ContractDeploymentScript extends NeoSerializable {

    private byte[] scriptBinary;

    private ContractDescriptionProperties descriptionProperties;

    private ContractFunctionProperties functionProperties;

    private ScriptHash contractScriptHash;

    public ContractDeploymentScript() {
    }

    public ContractDeploymentScript(byte[] scriptBinary,
            ContractFunctionProperties functionProperties,
            ContractDescriptionProperties descriptionProperties) {
        this.scriptBinary = scriptBinary;
        this.functionProperties = functionProperties;
        this.descriptionProperties = descriptionProperties;
        this.contractScriptHash = ScriptHash.fromScript(scriptBinary);
    }

    public byte[] getScriptBinary() {
        return scriptBinary;
    }

    public ContractDescriptionProperties getDescriptionProperties() {
        return descriptionProperties;
    }

    public ContractFunctionProperties getFunctionProperties() {
        return functionProperties;
    }

    public ScriptHash getContractScriptHash() {
        return contractScriptHash;
    }

    public BigDecimal getDeploymentSystemFee() {
        int fee = NeoConstants.CONTRACT_DEPLOY_BASIC_FEE;
        if (functionProperties.getNeedsStorage()) {
            fee += NeoConstants.CONTRACT_DEPLOY_STORAGE_FEE;
        }
        if (functionProperties.getNeedsDynamicInvoke()) {
            fee += NeoConstants.CONTRACT_DEPLOY_DYNAMIC_INVOKE_FEE;
        }
        fee -= NeoConstants.FREE_OF_CHARGE_EXECUTION_COST;
        return new BigDecimal(Math.max(fee, 0));
    }

    @Override
    public void deserialize(BinaryReader reader) throws DeserializationException {
        try {
            this.descriptionProperties = reader.readSerializable(
                    ContractDescriptionProperties.class);
            this.functionProperties = reader.readSerializable(ContractFunctionProperties.class);
            this.scriptBinary = reader.readPushData();
            this.contractScriptHash = ScriptHash.fromScript(this.scriptBinary);
        } catch (IllegalAccessException | InstantiationException | IOException e) {
            throw new DeserializationException(e);
        }
    }

    @Override
    public void serialize(BinaryWriter writer) throws IOException {
        // description properties (i.e., description,
        // email, author, version, name)
        writer.writeSerializableFixed(this.descriptionProperties);
        // function properties (i.e., parameter types, return type,
        // needs storage, needs dynamic invoke, is payable)
        writer.writeSerializableFixed(this.functionProperties);
        // script binary (.avm)
        writer.write(new ScriptBuilder()
                .pushData(this.scriptBinary)
                .toArray());
        // syscall "Neo.Contract.Create"
        writer.write(new ScriptBuilder()
                .sysCall(InteropServiceCode.NEO_CONTRACT_CREATE)
                .toArray());
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException();
    }

}
