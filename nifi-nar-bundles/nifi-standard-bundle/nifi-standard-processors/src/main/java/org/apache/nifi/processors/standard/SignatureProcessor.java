/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.standard;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.DataUnit;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

@Marks(categories = { "Convert & Control/Convert", "Network/Communication" }, createdDate = "2018-09-17")
@Tags({ "sign", "encryption" })
@CapabilityDescription("Digitally sign the specified content")
@WritesAttributes({ //
        @WritesAttribute(attribute = SignatureProcessor.ATTR_SIGNATURE, description = "The value of sign with special algorithm"), //
        @WritesAttribute(attribute = SignatureProcessor.ATTR_SIGN_ALG, description = "The name of sign algorithm"), //
        @WritesAttribute(attribute = SignatureProcessor.ATTR_REASON, description = "The reason for sign failed"),//
})
public class SignatureProcessor extends AbstractProcessor {
    public static final String ATTR_SIGNATURE = "signature";
    public static final String ATTR_SIGN_ALG = "sign.algorithm";
    public static final String ATTR_REASON = "sign.failed.reason";

    public enum ESA { // Enum for Sign Algorithm
        MD5("MD", MessageDigestAlgorithms.MD5), /* MD2("MD", MessageDigestAlgorithms.MD2), // */

        SHA1("SHA", MessageDigestAlgorithms.SHA_1), SHA224("SHA", MessageDigestAlgorithms.SHA_224, MessageDigestAlgorithms.SHA_224), SHA256("SHA", MessageDigestAlgorithms.SHA_256), SHA384("SHA",
                MessageDigestAlgorithms.SHA_384), SHA512("SHA", MessageDigestAlgorithms.SHA_512), //

        /*
         * SHA3_224("SHA3", MessageDigestAlgorithms.SHA3_224, MessageDigestAlgorithms.SHA3_224 + " JDK9+"), SHA3_256("SHA3", MessageDigestAlgorithms.SHA3_256, MessageDigestAlgorithms.SHA3_256 +
         * " JDK9+"), SHA3_384("SHA3", MessageDigestAlgorithms.SHA3_384, MessageDigestAlgorithms.SHA3_384 + " JDK9+"), SHA3_512("SHA3", MessageDigestAlgorithms.SHA3_512,
         * MessageDigestAlgorithms.SHA3_512 + " JDK9+"), //
         */

        SHA1DSA("DSA", "SHA1withDSA"), SHA224DSA("DSA", "SHA224withDSA"), SHA256DSA("DSA", "SHA256withDSA"), //

        SHA1RSA("RSA", "SHA1WithRSA"), SHA256RSA("RSA", "SHA256withRSA"), SHA512RSA("RSA", "SHA512withRSA"), MD5RSA("RSA", "MD5withRSA"), //

        SHA1EC("EC", "SHA1withECDSA"), SHA224EC("EC", "SHA224withECDSA"), SHA256EC("EC", "SHA256withECDSA"), SHA512EC("EC", "SHA512withECDSA"), NONEEC("EC", "NONEwithECDSA"), //

        SHA1MAC("MAC", "HmacSHA1"), SHA256MAC("MAC", "HmacSHA256"), SHA512MAC("MAC", "HmacSHA512"), MD5MAC("MAC", "HmacMD5"), //
        ;
        private String value;
        private String category;
        private String desc;

        private ESA() {
        }

        private ESA(String category) {
            this();
            this.category = category;
        }

        private ESA(String category, String value) {
            this(category);
            this.value = value;
        }

        private ESA(String category, String value, String desc) {
            this(category, value);
            this.desc = desc;
        }

        public String getCategory() {
            return StringUtils.isBlank(category) ? this.name() : category;
        }

        public String getValue() {
            return StringUtils.isBlank(value) ? this.name() : value;
        }

        public String getDesc() {
            return this.desc;
        }

        public static AllowableValue[] getAllowableValues() {
            return Arrays.asList(ESA.values()).stream().map(sa -> StringUtils.isBlank(sa.getDesc()) ? new AllowableValue(sa.getValue()) : new AllowableValue(sa.getValue(), sa.getDesc()))
                    .toArray(AllowableValue[]::new);
        }

        public static ESA get(String value) {
            final Optional<ESA> find = Arrays.asList(ESA.values()).stream().filter(sa -> sa.getValue().equals(value)).findFirst();
            if (find.isPresent()) {
                return find.get();
            }
            return null;
        }

    }

    public static final String SIGN_DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String SIGN_DESTINATION_CONTENT = "flowfile-content";

    public static final String SIGN_ENCODE_APACHE_HEX = "Hex";
    public static final String SIGN_ENCODE_APACHE_BASE64 = "Base64";
    public static final String SIGN_ENCODE_JAVA_BASE64 = "Java Base64";

    public static final PropertyDescriptor SIGN_ALGORITHM = new PropertyDescriptor.Builder()//
            .name("Sign Algorithm")//
            .description("Specifies the signature algorithm")//
            .required(true)//
            .allowableValues(ESA.getAllowableValues())//
            .defaultValue(ESA.MD5.getValue()) //
            .build();

    public static final PropertyDescriptor SIGN_DESTINATION = new PropertyDescriptor.Builder()//
            .name("Sign Destination")//
            .description("Specifies the signed content is placed flowfile's attribute or content, default is placed to attribute")//
            .required(true)//
            .allowableValues(SIGN_DESTINATION_ATTRIBUTE, SIGN_DESTINATION_CONTENT)//
            .defaultValue(SIGN_DESTINATION_ATTRIBUTE)//
            .build();

    public static final PropertyDescriptor READ_DATA_BUFFER_SIZE = new PropertyDescriptor.Builder()//
            .name("Read Data Buffer Size")//
            .description("Specifies the signed buffer size to read contents from incoming connection.")//
            .required(true)//
            .defaultValue("8 KB")//
            .addValidator(StandardValidators.DATA_SIZE_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)//
            .build();

    public static final PropertyDescriptor SECRET = new PropertyDescriptor.Builder()//
            .name("Private Key")//
            .description("Specifies the private key or secret key(for HMAC) for pkcs8 format. Except the MDx, SHA1 and SHA-XXX, others need this one.")//
            .required(false)//
            .defaultValue("")//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)//
            .build();

    public static final PropertyDescriptor SIGN_ENCODING_ALG = new PropertyDescriptor.Builder()//
            .name("Encoding Signature")//
            .description("Specifies encoding format for the signed content ")//
            .required(true)//
            .allowableValues(SIGN_ENCODE_APACHE_HEX, SIGN_ENCODE_APACHE_BASE64)//
            .defaultValue(SIGN_ENCODE_APACHE_HEX)//
            .build();

    // make sure old flow template to be ok, need keep this property
    public static final PropertyDescriptor CHARSET = new PropertyDescriptor.Builder()//
            .name("Character Set")//
            .description("(Deprecated) Specifies the character set of the received data. Will process data via bytes directly, so ignore this setting")//
            .required(false)//
            .defaultValue("UTF-8")//
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)//
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()//
            .name("success")//
            .description("Any FlowFile that is successfully signed will be routed to success")//
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()//
            .name("failure")//
            .description("Any FlowFile that signed failed will be routed to failure")//
            .build();

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;

    private volatile boolean signToAttribute;
    private volatile int readDataBufferSize;
    private volatile ESA signAlgorithm;
    private volatile String secretValue;
    private volatile String signEncodeAlgorithm;
    // private volatile Charset charset;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(SIGN_ALGORITHM);
        properties.add(SIGN_DESTINATION);
        properties.add(READ_DATA_BUFFER_SIZE);
        properties.add(SECRET);
        properties.add(SIGN_ENCODING_ALG);
        properties.add(CHARSET);
        this.properties = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        // charset = Charset.forName(context.getProperty(CHARSET).getValue());

        String destination = context.getProperty(SIGN_DESTINATION).getValue();
        signToAttribute = (destination.equalsIgnoreCase(SIGN_DESTINATION_ATTRIBUTE) || StringUtils.isEmpty(destination)) ? true : false;
        signAlgorithm = ESA.get(context.getProperty(SIGN_ALGORITHM).getValue());
        signEncodeAlgorithm = context.getProperty(SIGN_ENCODING_ALG).getValue();
    }

    private PrivateKey getPrivateKey(String algorithm) throws Exception {
        StringBuilder pkcs8Lines = new StringBuilder();
        BufferedReader rdr = new BufferedReader(new StringReader(secretValue));
        String line;
        while ((line = rdr.readLine()) != null) {
            pkcs8Lines.append(line.trim());
        }
        String pkcs8Pem = pkcs8Lines.toString();
        pkcs8Pem = pkcs8Pem.trim();
        pkcs8Pem = pkcs8Pem.replaceFirst("-----BEGIN(.*?)PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replaceFirst("-----END(.*?)PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");
        try {
            byte[] pkcs8EncodedBytes = org.apache.commons.codec.binary.Base64.decodeBase64(pkcs8Pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            PrivateKey privKey = kf.generatePrivate(keySpec);
            return privKey;
        } catch (Exception e) {
            throw new InvalidKeySpecException("Can't init the private key", e);
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        readDataBufferSize = context.getProperty(READ_DATA_BUFFER_SIZE).evaluateAttributeExpressions(flowFile).asDataSize(DataUnit.B).intValue();
        secretValue = context.getProperty(SECRET).evaluateAttributeExpressions(flowFile).getValue();

        final ComponentLog logger = getLogger();
        final StopWatch stopWatch = new StopWatch(true);

        try {
            if (signToAttribute) {
                final Map<String, String> signMap = new HashMap<>();
                session.read(flowFile, new InputStreamCallback() {
                    @Override
                    public void process(InputStream in) throws IOException {
                        try {

                            String sign = signContent(context, IOUtils.buffer(in));
                            signMap.put(ATTR_SIGNATURE, sign);
                        } catch (Exception e) {
                            throw new ProcessException(e);
                        }
                    }
                });
                flowFile = session.putAllAttributes(flowFile, signMap);
            } else {
                flowFile = session.write(flowFile, new StreamCallback() {

                    @Override
                    public void process(InputStream in, OutputStream out) throws IOException {
                        try {
                            String sign = signContent(context, IOUtils.buffer(in));
                            out.write(sign.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (Exception e) {
                            throw new ProcessException(e);
                        }
                    }
                });
                // change to text
                flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "text/plain;charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            }

            logger.info("Successfully sign {}", new Object[] { flowFile });

            flowFile = session.putAttribute(flowFile, ATTR_SIGN_ALG, signAlgorithm.getValue());
            session.getProvenanceReporter().modifyContent(flowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(flowFile, REL_SUCCESS);
        } catch (Exception e) {
            logger.error("Failed to sign {} due to {}", new Object[] { flowFile, e });

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (e.getCause() != null) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
            flowFile = session.putAttribute(flowFile, ATTR_REASON, sw.toString());

            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private String signContent(final ProcessContext context, BufferedInputStream contentStream) throws Exception {
        if (signAlgorithm.getCategory().equals(ESA.MD5.getCategory())
                || signAlgorithm.getCategory().equals(ESA.SHA1.getCategory())/* || signAlgorithm.getCategory().equals(ESA.SHA3_512.getCategory()) */) {
            return signWithMessageDigest(contentStream);
        } else {
            if (StringUtils.isBlank(secretValue)) {
                throw new IllegalArgumentException("Must provide the private key.");
            }

            if (signAlgorithm.getCategory().equals(ESA.SHA1MAC.getCategory())) {
                return signWithHMAC(contentStream);
            } else {
                return signWithAlgorithm(contentStream);
            }
        }
    }

    // sha mdx
    private String signWithMessageDigest(BufferedInputStream contentStream) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance(signAlgorithm.getValue());

            // same code like DigestUtils.updateDigest
            final byte[] buffer = new byte[readDataBufferSize];
            int read = contentStream.read(buffer, 0, readDataBufferSize);
            while (read > -1) {
                digest.update(buffer, 0, read);
                read = contentStream.read(buffer, 0, readDataBufferSize);
            }

            final byte[] signs = digest.digest();
            return encodeSignature(signs);

        } catch (Exception e) {
            getLogger().error("Failed to sign {} due to {}", new Object[] { signAlgorithm, e });
            throw e;
        }

    }

    // WithXXX
    private String signWithAlgorithm(BufferedInputStream contentStream) throws Exception {
        try {
            Signature signature = Signature.getInstance(signAlgorithm.getValue());
            PrivateKey privateKey = getPrivateKey(signAlgorithm.getCategory());
            signature.initSign(privateKey);

            final byte[] buffer = new byte[readDataBufferSize];
            int read = contentStream.read(buffer, 0, readDataBufferSize);
            while (read > -1) {
                signature.update(buffer, 0, read);
                read = contentStream.read(buffer, 0, readDataBufferSize);
            }

            final byte[] signs = signature.sign();
            return encodeSignature(signs);
        } catch (Exception e) {
            getLogger().error("Failed to sign {} due to {}", new Object[] { signAlgorithm, e });
            throw e;
        }
    }

    // hmac
    private String signWithHMAC(BufferedInputStream contentStream) throws Exception {
        try {
            SecretKeySpec secret_key = new SecretKeySpec(secretValue.getBytes(StandardCharsets.UTF_8), signAlgorithm.getValue());
            Mac hmac = Mac.getInstance(secret_key.getAlgorithm());
            hmac.init(secret_key);

            final byte[] buffer = new byte[readDataBufferSize];
            int read = contentStream.read(buffer, 0, readDataBufferSize);
            while (read > -1) {
                hmac.update(buffer, 0, read);
                read = contentStream.read(buffer, 0, readDataBufferSize);
            }

            // final byte[] signs = hmac.doFinal(IOUtils.toByteArray(contentStream));
            final byte[] signs = hmac.doFinal();
            return encodeSignature(signs);
        } catch (Exception e) {
            getLogger().error("Failed to sign {} due to {}", new Object[] { signAlgorithm, e });
            throw e;
        }
    }

    private String encodeSignature(final byte[] signValue) {
        if (SIGN_ENCODE_APACHE_BASE64.equals(signEncodeAlgorithm)) {
            return org.apache.commons.codec.binary.Base64.encodeBase64String(signValue);
        } else if (SIGN_ENCODE_JAVA_BASE64.equals(signEncodeAlgorithm)) {
            return java.util.Base64.getEncoder().encodeToString(signValue); // same as Apache Base64
        } else {
            // }else if(SIGN_ENCODE_APACHE_HEX.equals(signEncodeAlgorithm)) { //default
            return org.apache.commons.codec.binary.Hex.encodeHexString(signValue);
        }
    }

}
