package io.neow3j.transaction;

import static io.neow3j.constants.OpCode.PUSH2;
import static io.neow3j.constants.OpCode.PUSH3;
import static io.neow3j.constants.OpCode.PUSHBYTES33;
import static io.neow3j.utils.ArrayUtils.concatenate;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import io.neow3j.constants.InteropServiceCode;
import io.neow3j.constants.OpCode;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.io.NeoSerializableInterface;
import io.neow3j.io.exceptions.DeserializationException;
import io.neow3j.utils.ArrayUtils;
import io.neow3j.utils.Numeric;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class VerificationScriptTest {

    @Test
    public void testFromPublicKey() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {

        ECPublicKey key = ECKeyPair.createEcKeyPair().getPublicKey2();
        VerificationScript veriScript = new VerificationScript(key);

        byte[] expectedScript = concatenate(concatenate(concatenate(
                PUSHBYTES33.getValue(),
                key.getEncoded(true)),
                OpCode.SYSCALL.getValue()),
                InteropServiceCode.NEO_CRYPTO_CHECKSIG.getCodeBytes());

        assertArrayEquals(expectedScript, veriScript.getScript());
    }

    @Test
    public void testFromPublicKeys() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {

        List<ECPublicKey> publicKeys = new ArrayList<>();
        publicKeys.add(ECKeyPair.createEcKeyPair().getPublicKey2());
        publicKeys.add(ECKeyPair.createEcKeyPair().getPublicKey2());
        publicKeys.add(ECKeyPair.createEcKeyPair().getPublicKey2());

        ByteBuffer buf = ByteBuffer.allocate(1 + 3*(1 + 33) + 1 + 1);
        buf.put(PUSH2.getValue());
        buf.put(PUSHBYTES33.getValue());
        buf.put(publicKeys.get(0).getEncoded(true));
        buf.put(PUSHBYTES33.getValue());
        buf.put(publicKeys.get(1).getEncoded(true));
        buf.put(PUSHBYTES33.getValue());
        buf.put(publicKeys.get(2).getEncoded(true));
        buf.put(PUSH3.getValue());
        buf.put(OpCode.SYSCALL.getValue());
        buf.put(InteropServiceCode.NEO_CRYPTO_CHECKMULTISIG.getCodeBytes());
        VerificationScript script = new VerificationScript(publicKeys, 2);

        assertArrayEquals(buf.array(), script.getScript());
    }

    @Test
    public void testSerialize() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException {

        ECPublicKey key = ECKeyPair.createEcKeyPair().getPublicKey2();
        VerificationScript veriScript = new VerificationScript(key);

        byte[] expectedScript = ByteBuffer.allocate(1+1+33+1)
                .put((byte)35)
                .put(PUSHBYTES33.getValue())
                .put(key.getEncoded(true))
                .put(OpCode.SYSCALL.getValue())
                .put(InteropServiceCode.NEO_CRYPTO_CHECKSIG.getCodeBytes())
                .array();

        assertArrayEquals(expectedScript, veriScript.toArray());
    }

    @Test
    public void testDeserialize() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException, DeserializationException {

        int messageSize = 32;
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);
        byte[] serializedScript = ArrayUtils.concatenate((byte)messageSize, message);
        VerificationScript script = NeoSerializableInterface.from(serializedScript, VerificationScript.class);
        assertArrayEquals(message, script.getScript());

        ECKeyPair keyPair = ECKeyPair.createEcKeyPair();
        byte[] pub = ArrayUtils.concatenate(PUSHBYTES33.getValue(), keyPair.getPublicKey().toByteArray());
        byte[] expectedScript = concatenate(concatenate(concatenate(
                PUSHBYTES33.getValue(),
                keyPair.getPublicKey2().getEncoded(true)),
                OpCode.SYSCALL.getValue()),
                InteropServiceCode.NEO_CRYPTO_CHECKSIG.getCodeBytes());

        serializedScript = ArrayUtils.concatenate((byte)35, expectedScript);
        script = NeoSerializableInterface.from(serializedScript, VerificationScript.class);
        assertArrayEquals(expectedScript, script.getScript());

        messageSize = 256;
        message = new byte[messageSize];
        Arrays.fill(message, (byte)1);
        ByteBuffer buf = ByteBuffer.allocate(3 + messageSize);
        // Message size is bigger than one byte and needs encoding with byte 0xFD, which signifies
        // that a uint16 follows in little endian format, i.e. least significant byte first.
        buf.put((byte)0xFD);
        buf.put((byte)0x00);
        buf.put((byte)0x01);
        buf.put(message);
        script = NeoSerializableInterface.from(buf.array(), VerificationScript.class);
        assertArrayEquals(message, script.getScript());
    }
    
    @Test
    public void testGetSigningThreshold() {
        byte[] scriptBytes = Numeric.hexStringToByteArray("522102028a99826edc0c97d18e22b6932373d908d323aa7f92656a77ec26e8861699ef21031d8e1630ce640966967bc6d95223d21f44304133003140c3b52004dc981349c92102232ce8d2e2063dce0451131851d47421bfc4fc1da4db116fca5302c0756462fa53ae");
        int th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(2, th);

        scriptBytes = Numeric.hexStringToByteArray("532102028a99826edc0c97d18e22b6932373d908d323aa7f92656a77ec26e8861699ef21031d8e1630ce640966967bc6d95223d21f44304133003140c3b52004dc981349c92102232ce8d2e2063dce0451131851d47421bfc4fc1da4db116fca5302c0756462fa53ae");
        th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(3, th);

        scriptBytes = Numeric.hexStringToByteArray("60ae");
        th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(16, th);

        scriptBytes = Numeric.hexStringToByteArray("02ff00ae");
        th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(255, th);

        scriptBytes = Numeric.hexStringToByteArray("020001ae");
        th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(256, th);

        scriptBytes = Numeric.hexStringToByteArray("020004ae");
        th = new VerificationScript(scriptBytes).getSigningThreshold();
        assertEquals(1024, th);
    }

    @Test
    public void getSize() {
        byte[] script = Numeric.hexStringToByteArray(""
            + "147e5f3c929dd830d961626551dbea6b70e4b2837ed2fe9089eed2072ab3a655"
            + "523ae0fa8711eee4769f1913b180b9b3410bbb2cf770f529c85f6886f22cbaaf");
        InvocationScript s = new InvocationScript(script);
        assertThat(s.getSize(), is(1 + 64)); // byte for script length and actual length.
    }

}