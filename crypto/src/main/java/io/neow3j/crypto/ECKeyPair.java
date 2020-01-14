package io.neow3j.crypto;

import static io.neow3j.constants.NeoConstants.PRIVATE_KEY_SIZE;
import static io.neow3j.constants.NeoConstants.PUBLIC_KEY_SIZE;
import static io.neow3j.crypto.SecurityProviderChecker.addBouncyCastle;

import io.neow3j.constants.NeoConstants;
import io.neow3j.contract.ScriptBuilder;
import io.neow3j.contract.ScriptHash;
import io.neow3j.io.BinaryReader;
import io.neow3j.io.BinaryWriter;
import io.neow3j.io.NeoSerializable;
import io.neow3j.io.exceptions.DeserializationException;
import io.neow3j.utils.ArrayUtils;
import io.neow3j.utils.KeyUtils;
import io.neow3j.utils.Numeric;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;


/**
 * Elliptic Curve SECP-256r1 generated key pair.
 */
public class ECKeyPair {

    private final BigInteger privateKey;
    private final ECPublicKey publicKey;

    static {
        addBouncyCastle();
    }

    // TODO: Remove as soon as private key is also its own type (ECPrivateKey).
    public ECKeyPair(BigInteger privateKey, BigInteger publicKey) {
        this.privateKey = privateKey;
        this.publicKey = new ECPublicKey(publicKey);
    }

    // TODO: Remove as soon as private key is also its own type (ECPrivateKey).
    public ECKeyPair(BigInteger privateKey, ECPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }


    public BigInteger getPrivateKey() {
        return privateKey;
    }

    // TODO: Remove and fix all occurences
    public BigInteger getPublicKey() {
        return Numeric.toBigInt(publicKey.getEncoded(true));
    }

    // TODO: Rename after removing the above method.
    public ECPublicKey getPublicKey2() {
        return publicKey;
    }

    /**
     * Constructs the NEO address from this key pairs public key.
     * The address is constructed ad hoc each time this method is called.
     *
     * @return the NEO address of the public key.
     */
    public String getAddress() {
        byte[] script = ScriptBuilder.buildVerificationScript(this.publicKey.getEncoded(true));
        return ScriptHash.fromScript(script).toAddress();
    }

    /**
     * Sign a hash with the private key of this key pair.
     *
     * @param transactionHash the hash to sign
     * @return A raw {@link BigInteger} array with the signature
     */
    public BigInteger[] sign(byte[] transactionHash) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKey, NeoConstants.CURVE);
        signer.init(true, privKey);
        return signer.generateSignature(transactionHash);
    }

    /**
     * Sign a hash with the private key of this key pair.
     *
     * @param transactionHash the hash to sign
     * @return An {@link ECDSASignature} of the hash
     */
    public ECDSASignature signAndGetECDSASignature(byte[] transactionHash) {
        BigInteger[] components = sign(transactionHash);
        // in bitcoin and ethereum we would/could use .toCanonicalised(), but not in NEO, AFAIK
        return new ECDSASignature(components[0], components[1]);
    }

    /**
     * Sign a hash with the private key of this key pair.
     *
     * @param transactionHash the hash to sign
     * @return A byte array with the canonicalized signature
     */
    public byte[] signAndGetArrayBytes(byte[] transactionHash) {
        BigInteger[] components = sign(transactionHash);
        byte[] signature = new byte[64];
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, components[0]), 0, signature, 0, 32);
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, components[1]), 0, signature, 32, 32);
        return signature;
    }

    public static ECKeyPair create(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();

        BigInteger privateKeyValue = privateKey.getD();

        byte[] publicKeyBytes = publicKey.getQ().getEncoded(true);
        BigInteger publicKeyValue = new BigInteger(1, publicKeyBytes);

        return new ECKeyPair(privateKeyValue, publicKeyValue);
    }

    public static ECKeyPair create(BigInteger privateKey) {
        return new ECKeyPair(privateKey, Sign.publicKeyFromPrivate(privateKey));
    }

    public static ECKeyPair create(byte[] privateKey) {
        return create(Numeric.toBigInt(privateKey));
    }

    /**
     * <p>Create a keypair using SECP-256r1 curve.</p>
     * <br>
     * <p>Private keypairs are encoded using PKCS8.</p>
     * <br>
     * <p>Private keys are encoded using X.509.</p>
     *
     * @return The created {@link ECKeyPair}.
     * @throws InvalidAlgorithmParameterException throws if the algorithm parameter used is invalid.
     * @throws NoSuchAlgorithmException           throws if the encryption algorithm is not available in the specified provider.
     * @throws NoSuchProviderException            throws if the provider is not available.
     */
    public static ECKeyPair createEcKeyPair() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = createSecp256r1KeyPair();
        return create(keyPair);
    }

    private static KeyPair createSecp256r1KeyPair() throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);

        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256r1");
        keyPairGenerator.initialize(ecGenParameterSpec, SecureRandomUtils.secureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public String exportAsWIF() {
        byte[] data = ArrayUtils.concatenate(
                new byte[]{(byte) 0x80},
                Numeric.toBytesPadded(getPrivateKey(), PRIVATE_KEY_SIZE),
                new byte[]{(byte) 0x01}
        );
        byte[] checksum = Hash.sha256(Hash.sha256(data, 0, data.length));
        byte[] first4Bytes = Arrays.copyOfRange(checksum, 0, 4);
        data = ArrayUtils.concatenate(data, first4Bytes);
        String wif = Base58.encode(data);
        Arrays.fill(data, (byte) 0);
        return wif;
    }

    public byte[] serialize() {
        byte[] privateKey = KeyUtils.privateKeyIntegerToByteArray(this.getPublicKey());
        byte[] publicKey = KeyUtils.publicKeyIntegerToByteArray(this.getPublicKey());

        byte[] result = Arrays.copyOf(privateKey, PRIVATE_KEY_SIZE + PUBLIC_KEY_SIZE);
        System.arraycopy(publicKey, 0, result, PRIVATE_KEY_SIZE, PUBLIC_KEY_SIZE);
        return result;
    }

    public static ECKeyPair deserialize(byte[] input) {
        if (input.length != PRIVATE_KEY_SIZE + PUBLIC_KEY_SIZE) {
            throw new RuntimeException("Invalid input key size");
        }

        BigInteger privateKey = Numeric.toBigInt(input, 0, PRIVATE_KEY_SIZE);
        BigInteger publicKey = Numeric.toBigInt(input, PRIVATE_KEY_SIZE, PUBLIC_KEY_SIZE);

        return new ECKeyPair(privateKey, publicKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ECKeyPair ecKeyPair = (ECKeyPair) o;

        if (privateKey != null
                ? !privateKey.equals(ecKeyPair.privateKey) : ecKeyPair.privateKey != null) {
            return false;
        }

        return publicKey != null
                ? publicKey.equals(ecKeyPair.publicKey) : ecKeyPair.publicKey == null;
    }

    @Override
    public int hashCode() {
        int result = privateKey != null ? privateKey.hashCode() : 0;
        result = 31 * result + (publicKey != null ? publicKey.hashCode() : 0);
        return result;
    }

    // TODO: Implement
    public static class ECPrivateKey extends NeoSerializable {

        @Override
        public void deserialize(BinaryReader reader) throws DeserializationException {

        }

        @Override
        public void serialize(BinaryWriter writer) throws IOException {

        }

        @Override
        public int getSize() {
            return 0;
        }
    }

    public static class ECPublicKey extends NeoSerializable {

        private ECPoint ecPoint;

        public ECPublicKey() {
        }

        /**
         * Creates a new
         * @param ecPoint
         */
        public ECPublicKey(ECPoint ecPoint) {
            if (!ecPoint.getCurve().equals(NeoConstants.CURVE_PARAMS.getCurve())) {
                throw new IllegalArgumentException("Given EC point is not of the required curve.");
            }
            this.ecPoint = ecPoint;
        }

        /**
         * Creates a new instance from the given encoded public key. The public key must be encoded
         * as defined in section 2.3.3 of <a href="http://www.secg.org/sec1-v2.pdf">SEC1</a>. It can
         * be in compressed or uncompressed format.
         *
         * @param publicKey The public key.
         */
        public ECPublicKey(byte[] publicKey) {
            if (publicKey.length != NeoConstants.PUBLIC_KEY_SIZE) {
                throw new IllegalArgumentException("Public key argument must be " +
                    NeoConstants.PUBLIC_KEY_SIZE + " long but was " + publicKey.length + " bytes");
            }
            this.ecPoint = decodePoint(publicKey);
        }

        /**
         * Creates a new instance from the given encoded public key. The public key must be encoded
         * as defined in section 2.3.3 of <a href="http://www.secg.org/sec1-v2.pdf">SEC1</a>. It can
         * be in compressed or uncompressed format.
         *
         * @param publicKey The public key.
         */
        public ECPublicKey(BigInteger publicKey) {
            this(Numeric.toBytesPadded(publicKey, NeoConstants.PUBLIC_KEY_SIZE));
        }

        /**
         * Gets this public key's elliptic curve point encoded as defined in section 2.3.3 of
         * <a href="http://www.secg.org/sec1-v2.pdf">SEC1</a>.
         *
         * @param compressed If the EC point should be encoded in compressed or uncompressed format.
         * @return the encoded public key.
         */
        public byte[] getEncoded(boolean compressed) {
            return ecPoint.getEncoded(compressed);
        }

        public java.security.spec.ECPoint getECPoint() {
            ECPoint normPoint = this.ecPoint.normalize();
            return new java.security.spec.ECPoint(
                    normPoint.getAffineXCoord().toBigInteger(),
                    normPoint.getAffineYCoord().toBigInteger());
        }

        @Override
        public void deserialize(BinaryReader reader) throws DeserializationException {
            try {
                ecPoint = decodePoint(reader.readBytes(PUBLIC_KEY_SIZE));
            } catch (IOException e) {
                throw new DeserializationException();
            }
        }

        @Override
        public void serialize(BinaryWriter writer) throws IOException {
            writer.write(getEncoded(true));
        }

        @Override
        public int getSize() {
            return this.ecPoint.isInfinity() ? 1 : NeoConstants.PUBLIC_KEY_SIZE;
        }

        private ECPoint decodePoint(byte[] encodedPoint) {
            return NeoConstants.CURVE.getCurve().decodePoint(encodedPoint);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ECPublicKey that = (ECPublicKey) o;
            return Objects.equals(ecPoint, that.ecPoint);
        }
    }
}
