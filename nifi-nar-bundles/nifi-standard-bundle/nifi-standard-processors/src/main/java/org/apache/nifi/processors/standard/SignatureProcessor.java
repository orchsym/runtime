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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Base64;
import java.io.BufferedReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.binary.Hex;

import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Marks(categories={"Convert & Control/Convert", "Network/Communication"}, createdDate="2018-09-17")
@Tags({"sign", "encryption"})
@CapabilityDescription("Digitally sign the specified content")
public class SignatureProcessor extends AbstractProcessor {

    public static final String SIGN_MD5 = "MD5";
    public static final String SIGN_SHA1 = "SHA1";
    public static final String SIGN_SHA256 = "SHA-256";
    public static final String SIGN_SHA512 = "SHA-512";
    public static final String SIGN_DSASHA1 = "SHA1withDSA";
    public static final String SIGN_DSASHA224 = "SHA224withDSA";
    public static final String SIGN_DSASHA256 = "SHA256withDSA";
    public static final String SIGN_ECDSANONE = "NONEwithECDSA";
    public static final String SIGN_ECDSASHA1 = "SHA1withECDSA";
    public static final String SIGN_ECDSASHA224 = "SHA224withECDSA";
    public static final String SIGN_ECDSASHA256 = "SHA256withECDSA";
    public static final String SIGN_ECDSASHA512 = "SHA512withECDSA";
    public static final String SIGN_RSAMD5 = "MD5withRSA";
    public static final String SIGN_RSASHA1 = "SHA1WithRSA";
    public static final String SIGN_RSASHA256= "SHA256withRSA";
    public static final String SIGN_RSASHA512 = "SHA512withRSA";
    public static final String SIGN_HMACMD5 = "HmacMD5";
    public static final String SIGN_HMACSHA1 = "HmacSHA1";
    public static final String SIGN_HMACSHA256 = "HmacSHA256";
    public static final String SIGN_HMACSHA512 = "HmacSHA512";

    public static final String SIGN_DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String SIGN_DESTINATION_CONTENT = "flowfile-content";

    public static final PropertyDescriptor CHARSET = new PropertyDescriptor.Builder()
        .name("Character Set")
        .description("Specifies the character set of the received data.")
        .required(true)
        .defaultValue("UTF-8")
        .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
        .build();

    public static final PropertyDescriptor SECRET = new PropertyDescriptor.Builder()
        .name("Private Key")
        .description("Specifies the private key. If use rsa sign algorithm, the key is pkcs8 format")
        .required(false)
        .defaultValue("")
        .addValidator(Validator.VALID)
        .build();

    public static final PropertyDescriptor SIGN_ALGORITHM = new PropertyDescriptor.Builder()
        .name("Sign Algorithm")
        .description("Specifies the signature algorithm")
        .required(false)
        .allowableValues(SIGN_MD5, SIGN_SHA1, SIGN_SHA256, SIGN_SHA512, SIGN_RSAMD5, SIGN_RSASHA1, SIGN_RSASHA256, SIGN_RSASHA512, SIGN_HMACMD5, SIGN_HMACSHA1, SIGN_HMACSHA256, SIGN_HMACSHA512, SIGN_DSASHA1, SIGN_DSASHA224, SIGN_DSASHA256, SIGN_ECDSANONE, SIGN_ECDSASHA1, SIGN_ECDSASHA224, SIGN_ECDSASHA256, SIGN_ECDSASHA512)
        .build();

    public static final PropertyDescriptor SIGN_DESTINATION = new PropertyDescriptor.Builder()
        .name("Sign Destination")
        .description("Specifies the signed content is placed flowfile's attribute or content, default is placed to attribute")
        .required(false)
        .allowableValues(SIGN_DESTINATION_ATTRIBUTE, SIGN_DESTINATION_CONTENT)
        .defaultValue(SIGN_DESTINATION_ATTRIBUTE)
        .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("Any FlowFile that is successfully signed will be routed to success")
        .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("Any FlowFile that signed failed will be routed to failure")
        .build();

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(CHARSET);
        properties.add(SECRET);
        properties.add(SIGN_ALGORITHM);
        properties.add(SIGN_DESTINATION);
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

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        final ComponentLog logger = getLogger();
        try{
            final Charset charset = Charset.forName(context.getProperty(CHARSET).getValue());
            String destination = context.getProperty(SIGN_DESTINATION).getValue();
            boolean signToAttribute = (destination.equalsIgnoreCase(SIGN_DESTINATION_ATTRIBUTE)||StringUtils.isEmpty(destination)) ? true : false;
            final StopWatch stopWatch = new StopWatch(true);
            if (signToAttribute) {
                final Map<String, String> signMap = new HashMap();
                session.read(flowFile, new InputStreamCallback() {
                    @Override
                    public void process(InputStream in) throws IOException {
                        try {
                            String content = getInputStreamContent(in, charset);
                            String sign = signContent(context, content, charset);
                            signMap.put("signature", sign);
                        }catch (Exception e) {
                            throw new ProcessException(e);
                        }
                    }
                });
                flowFile = session.putAllAttributes(flowFile, signMap);
            } else {
                flowFile = session.write(flowFile, new StreamCallback() {
                    @Override
                    public void process(InputStream in, OutputStream out) throws IOException {
                        try{
                            String content = getInputStreamContent(in, charset);
                            String sign = signContent(context, content, charset);
                            out.write(sign.getBytes(charset));
                            out.flush(); 
                        }catch (Exception e) {
                            throw new ProcessException(e);
                        }
                    }
                });
            }

            logger.info("Successfully sign {}", new Object[] {flowFile});
            session.getProvenanceReporter().modifyContent(flowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(flowFile, REL_SUCCESS);
        } catch (ProcessException e) {
            logger.error("Failed to sign {} due to {}", new Object[] {flowFile, e});
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private String getInputStreamContent(InputStream in, Charset charset) throws Exception{
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, charset);
        String content = writer.toString();
        return content;
    }

    private String signContent(final ProcessContext context,String content, Charset charset) {
        String sign = "";
        String algorithm = context.getProperty(SIGN_ALGORITHM).getValue();
        if (StringUtils.isEmpty(algorithm)) {
            return sign;
        }
        String privateKey =  context.getProperty(SECRET).getValue();
        
        switch (algorithm) {
            case SIGN_MD5:
                sign = signWithMessageDigest(content, SIGN_MD5, charset);
                break;
            //sha
            case SIGN_SHA1:
                sign = signWithMessageDigest(content, SIGN_SHA1, charset);
                break;
            case SIGN_SHA256:
                sign = signWithMessageDigest(content, SIGN_SHA256, charset);
                break;
            case SIGN_SHA512:
                sign = signWithMessageDigest(content, SIGN_SHA512, charset);
                break;
            //rsa
            case SIGN_RSAMD5:
                sign = signWithAlgorithm(content, privateKey, "RSA", SIGN_RSAMD5, charset);
                break;
            case SIGN_RSASHA1:
                sign = signWithAlgorithm(content, privateKey, "RSA", SIGN_RSASHA1, charset);
                break;
            case SIGN_RSASHA256:
                sign = signWithAlgorithm(content, privateKey, "RSA", SIGN_RSASHA256, charset);
                break;
            case SIGN_RSASHA512:
                sign = signWithAlgorithm(content, privateKey, "RSA", SIGN_RSASHA512, charset);
                break;
            //dsa
            case SIGN_DSASHA1:
                sign = signWithAlgorithm(content, privateKey, "DSA" ,SIGN_DSASHA1, charset);
                break;
            case SIGN_DSASHA224:
                sign = signWithAlgorithm(content, privateKey, "DSA" ,SIGN_DSASHA224, charset);
                break;
            case SIGN_DSASHA256:
                sign = signWithAlgorithm(content, privateKey, "DSA" ,SIGN_DSASHA256, charset);
                break;
            //ecdsa
            case SIGN_ECDSANONE:
                sign = signWithAlgorithm(content, privateKey, "EC" ,SIGN_ECDSANONE, charset);
                break;
            case SIGN_ECDSASHA1:
                sign = signWithAlgorithm(content, privateKey, "EC" ,SIGN_ECDSASHA1, charset);
                break;
            case SIGN_ECDSASHA224:
                sign = signWithAlgorithm(content, privateKey, "EC" ,SIGN_ECDSASHA224, charset);
                break;
            case SIGN_ECDSASHA256:
                sign = signWithAlgorithm(content, privateKey, "EC" ,SIGN_ECDSASHA256, charset);
                break;
            case SIGN_ECDSASHA512:
                sign = signWithAlgorithm(content, privateKey, "EC" ,SIGN_ECDSASHA512, charset);
                break;
            //hmac
            case SIGN_HMACMD5:
                sign = signWithHMAC(content, privateKey, SIGN_HMACMD5, charset);
                break;
            case SIGN_HMACSHA1:
                sign = signWithHMAC(content, privateKey, SIGN_HMACSHA1, charset);
                break;
            case SIGN_HMACSHA256:
                sign = signWithHMAC(content, privateKey, SIGN_HMACSHA256, charset);
                break;
            case SIGN_HMACSHA512:
                sign = signWithHMAC(content, privateKey, SIGN_HMACSHA512, charset);
                break;
            default:
                break;
        }
        return sign;
    }

    private PrivateKey getPrivateKey(String key, String algorithm) throws Exception {
        StringBuilder pkcs8Lines = new StringBuilder();
        BufferedReader rdr = new BufferedReader(new StringReader(key));
        String line;
        while ((line = rdr.readLine()) != null) {
            pkcs8Lines.append(line);
        }
        String pkcs8Pem = pkcs8Lines.toString();
        pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replaceAll("\\s+","");
        byte [] pkcs8EncodedBytes = Base64.getDecoder().decode(pkcs8Pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        PrivateKey privKey = kf.generatePrivate(keySpec);
        return privKey;
    }

    private String signWithAlgorithm(String content, String privateKey, String algorithm, String signAlgorithm ,Charset charset) {
        String signStr = "";
        try {
            byte[] contentBytes = content.getBytes(charset.name());
            Signature signature = Signature.getInstance(signAlgorithm);
            PrivateKey key = getPrivateKey(privateKey, algorithm);
            signature.initSign(key);
            signature.update(contentBytes);
            byte[] signs = signature.sign();
            signStr = Base64.getEncoder().encodeToString(signs);
        } catch (Exception e) {
            getLogger().error("Failed to {} sign {} due to {}", new Object[] {signAlgorithm , content, e});
        }
        return signStr;
    }

    //sha md5
    private String signWithMessageDigest(String content, String algorithm, Charset charset) {
        String signStr = "";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(content.getBytes(charset.name()));
            byte[] digest = md.digest();

            StringBuffer hexstr = new StringBuffer();
            String shaHex = "";
            for (int i = 0; i < digest.length; i++) {
                shaHex = Integer.toHexString(digest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexstr.append(0);
                }
                hexstr.append(shaHex);
            }
            signStr = hexstr.toString();
        } catch (Exception e) {
            getLogger().error("Failed to {} sign {} due to {}", new Object[] {algorithm, content, e});
        }
        return signStr;
        
    }

    //hmac
    private String signWithHMAC(String content, String privateKey, String algorithm, Charset charset) {
        String signStr = "";
        try {
                Mac hmac = Mac.getInstance(algorithm);
                SecretKeySpec secret_key = new SecretKeySpec(privateKey.getBytes(charset.name()), algorithm);
                hmac.init(secret_key);
                signStr = Hex.encodeHexString(hmac.doFinal(content.getBytes(charset.name())));
        } catch (Exception e) {
            getLogger().error("Failed to {} sign {} due to {}", new Object[] {algorithm, content, e});
        }
        return signStr;
    }

}
