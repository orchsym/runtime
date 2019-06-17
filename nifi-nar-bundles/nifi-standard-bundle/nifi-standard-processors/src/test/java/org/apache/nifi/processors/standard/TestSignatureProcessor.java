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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.nifi.processors.standard.SignatureProcessor.ESA;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * if use TestNG or JUnit 5, should be better.
 */
public class TestSignatureProcessor {
    private static final String testData = "Hello world!";

    private static final String rsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDAy5fSenxybCd3\n"
            + "PY61qels6oofEAGPTk6RjDJumneTCEH5h2Qfrrgr4v+TrIuwx+wt2OR7yeGaMBtC\n" + "ywRrnlwMwq3wyYxyjB0azGcGZty+1PwrYg4tzkhv+C1vAGR5a7PgAGBcLhY3dXGc\n"
            + "FoCOLFBLQeawiwKlLt13+I1Ysk9xcc9SG7ptrCDZOmmZ/fxv2I7Di1m1HKlBM07j\n" + "CusTKWkf5qglL1P07+A6rRBwLjpBnM0sdOq0uxl0JUkefe5u8qptFhFiU9JhVvd8\n"
            + "oMcXJUBrgmL2CFnjD837Jn79jiNKqa8GXjuauzQtqterxZ/+c7ZW7NfOHyyuoWFT\n" + "cwS+sAzzAgMBAAECggEAdvoti1rDielswY1fkIR25RwoeNrr24A7xF58kP0KZdZm\n"
            + "wJLpGxQ72/mON5f37PSzr+d1uV7wtrKRYDrhU3i63bUWms4Gunn1TCBwQ+ceuzW9\n" + "GRi+H056LX8+qo3Xc4cfzlDtJnXaiAcWQXkxhIucUZUtxC1FSKMPUYY11FLV6+94\n"
            + "ME9jw8slQJtQatxZRfMuL7mG+1Yn1IszQ2S2TcRvqTZkDyzxzNvebyZfp9Pfp2X8\n" + "mYNFEu8gCpfqcO72skgg031EGpX60ahZX3Jsm1zN0q93lJZMHPfTwtvQmtqFIV5n\n"
            + "c6auJpfjPUrteWa9YE10YODoQoP+30P3mjphOH2igQKBgQD5gyNzvLtQjqhQiKWq\n" + "wcGVbhcAqzM8KmiDE+jRCGsCwFzFEhL+aogr+311Gqinc5M+cbBouV/Eb+4MtNIL\n"
            + "tOduit8q2sn181w23vgMVjSVb8cG+QQ2RuSr9K17338DCTI0AXNLyJ8WS7LjtELr\n" + "k0AhSxJZVOXq/nckT2fnKrlO4wKBgQDFzuv+971IbmszlpgFTbKVWy2qW5Vl1KpJ\n"
            + "rbZb2waGSjt90HCDdTPa9zlwJrw9iQHpRfjZ2Zr3En71Qtk799ra4557ojyhHA14\n" + "IQPgnplCZNkYQoE8U3PrmQv9/HDobZnB2UEcczOTZ+RjpY0+xIJE7B7mDpqZZc+A\n"
            + "moBfAmMWsQKBgDE1QL+poBOSHvXLDUkw7znGPXkfSnp6LqRzP75B98DSKTmgh+hv\n" + "IC30ali3Cj4EGCz/hPgSXyXBoWQWITq30dDGSJ8OCTidZzlXHfpo4fP62HiykbcW\n"
            + "ojCbj79XY5g4rxL8mj1+8okWItCvk2ccYlBums7NZI7E4sBhfK9liKhbAoGBAKuV\n" + "IgC4xOffX/4P0y5Gh3Im9SWg66a5Ij1nAZgMFUzHd3NbKz3cnG6DpL8z2nVGMvI2\n"
            + "T/YoW9OVp6r3oZfzF1RnvUFhKVZXDxf1C3f8GtUMxYPVrGS3vwXObCiUoRv8djNA\n" + "UMps44ApzmLhZ/PaYWWHrAXDBTeqqKPVYSUI3QsBAoGAGMjpLf5wyroxgJPd808V\n"
            + "BoLCnMwmh7PRBuH/4BwD3tTUWGSaPjCMlXUhUDB1nl7Xyg/5Fuwxu/2+suq7ISS+\n" + "7EffRLWvffHZCczpt+IkFH0hFAVDikqjXfXJKLoGOwrv0vQkOllHo2Co4nZqchls\n" + "sl72uruxBxqo6Pq3zQBMXCc=\n"
            + "-----END RSA PRIVATE KEY-----";

    private static final String dsaPrivateKey = "-----BEGIN DSA PRIVATE KEY-----\n" + "MIHHAgEAMIGoBgcqhkjOOAQBMIGcAkEA/KaCzo4Syrom78z3EQ5SbbB4sF7ey80e\n"
            + "tKII864WF64B81uRpH5t9jQTxeEu0ImbzRMqzVDZkVG9xD7nN1kuFwIVAJYu3cw2\n" + "nLqOuyYO5rahJtk0bjjFAkBnhHGyepz0TukaScUUfbGpqvJE8FpDTWSGkx0tFCcb\n"
            + "njUDC3H9c9oXkGmzLik1Yw4cIGI1TQ2iCmxBblC+eUykBBcCFQCUrOU0i+GZt/Q0\n" + "WE4frngBb+AS1w==\n" + "-----END DSA PRIVATE KEY-----";

    private static final String ecPrivateKey = "-----BEGIN EC PRIVATE KEY-----\n" + "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDCBSIMU19gYqIt1crL\n" + "59AVXleilIOoMKpGRa+a0k6ezg==\n"
            + "-----END EC PRIVATE KEY-----";

    private static final String secret = "password";

    private void assertAttrRunner(TestRunner runner, String signValue) {
        runner.enqueue(testData);
        runner.run();

        runner.assertTransferCount(SignatureProcessor.REL_SUCCESS, 1);
        runner.assertQueueEmpty();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(SignatureProcessor.REL_SUCCESS);
        Assert.assertEquals(1, flowFiles.size());

        final MockFlowFile mockFlowFile = flowFiles.get(0);
        mockFlowFile.assertContentEquals(testData);
        mockFlowFile.assertAttributeEquals(SignatureProcessor.ATTR_SIGNATURE, signValue);
    }

    private void assertAttrAlgRunner(ESA alg, String signValue, String encodeAlg) {
        final TestRunner runner = TestRunners.newTestRunner(new SignatureProcessor());
        runner.setProperty(SignatureProcessor.SIGN_ALGORITHM, alg.getValue());
        runner.setProperty(SignatureProcessor.SIGN_DESTINATION, SignatureProcessor.SIGN_DESTINATION_ATTRIBUTE);
        runner.setProperty(SignatureProcessor.SIGN_ENCODING_ALG, encodeAlg);
        assertAttrRunner(runner, signValue);
    }

    private void assertAttrAlgCecretRunner(ESA alg, String signValue, String encodeAlg) {
        final TestRunner runner = TestRunners.newTestRunner(new SignatureProcessor());
        runner.setProperty(SignatureProcessor.SIGN_ALGORITHM, alg.getValue());
        runner.setProperty(SignatureProcessor.SIGN_DESTINATION, SignatureProcessor.SIGN_DESTINATION_ATTRIBUTE);
        runner.setProperty(SignatureProcessor.SIGN_ENCODING_ALG, encodeAlg);

        if (alg.getCategory().equals(ESA.SHA1RSA.getCategory())) {
            runner.setProperty(SignatureProcessor.SECRET, rsaPrivateKey);
        } else if (alg.getCategory().equals(ESA.SHA1EC.getCategory())) {
            runner.setProperty(SignatureProcessor.SECRET, ecPrivateKey);
        } else if (alg.getCategory().equals(ESA.SHA1DSA.getCategory())) {
            runner.setProperty(SignatureProcessor.SECRET, dsaPrivateKey);
        } else if (alg.getCategory().equals(ESA.SHA1MAC.getCategory())) {
            runner.setProperty(SignatureProcessor.SECRET, secret);
        }

        assertAttrRunner(runner, signValue);
    }

    private void assertUnTestESA(Map<ESA, String> signMap) {
        List<String> unTests = signMap.keySet().stream().map(sa -> sa.getCategory()).distinct()//
                .map(c -> Arrays.asList(ESA.values()).stream().filter(sa -> sa.getCategory().equals(c)).collect(Collectors.toList())).flatMap(list -> list.stream())
                .filter(sa -> !signMap.containsKey(sa)).map(sa -> sa.getValue()).distinct().collect(Collectors.toList());

        Assert.assertEquals("Some are missing to test:" + String.join(",", unTests), 0, unTests.size());
    }

    @Test
    public void testDefaultSetting() {
        final TestRunner runner = TestRunners.newTestRunner(new SignatureProcessor());

        Assert.assertEquals(DigestUtils.md5Hex(testData), "86fb269d190d2c85f6e0468ceca42a20");

        assertAttrRunner(runner, "86fb269d190d2c85f6e0468ceca42a20");
    }

    @Test
    public void test_MD5_Content() {
        final TestRunner runner = TestRunners.newTestRunner(new SignatureProcessor());
        runner.setProperty(SignatureProcessor.SIGN_DESTINATION, SignatureProcessor.SIGN_DESTINATION_CONTENT);

        Assert.assertEquals(DigestUtils.md5Hex(testData), "86fb269d190d2c85f6e0468ceca42a20");

        runner.enqueue(testData);
        runner.run();

        runner.assertTransferCount(SignatureProcessor.REL_SUCCESS, 1);
        runner.assertQueueEmpty();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(SignatureProcessor.REL_SUCCESS);
        Assert.assertEquals(1, flowFiles.size());

        final MockFlowFile mockFlowFile = flowFiles.get(0);
        mockFlowFile.assertContentEquals("86fb269d190d2c85f6e0468ceca42a20");
        mockFlowFile.assertAttributeNotExists(SignatureProcessor.ATTR_SIGNATURE);
        mockFlowFile.assertAttributeEquals(SignatureProcessor.ATTR_SIGN_ALG, ESA.MD5.getValue());
        mockFlowFile.assertAttributeNotExists(SignatureProcessor.ATTR_REASON);
    }

    @Test
    public void testAttr_ApacheHex() {
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.MD5, "86fb269d190d2c85f6e0468ceca42a20");

        Assert.assertEquals(DigestUtils.sha1Hex(testData), "d3486ae9136e7856bc42212385ea797094475802");
        signMap.put(ESA.SHA1, "d3486ae9136e7856bc42212385ea797094475802");

        signMap.put(ESA.SHA224, "7e81ebe9e604a0c97fef0e4cfe71f9ba0ecba13332bde953ad1c66e4");
        signMap.put(ESA.SHA256, "c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a");
        signMap.put(ESA.SHA384, "86255fa2c36e4b30969eae17dc34c772cbebdfc58b58403900be87614eb1a34b8780263f255eb5e65ca9bbb8641cccfe");
        signMap.put(ESA.SHA512, "f6cde2a0f819314cdde55fc227d8d7dae3d28cc556222a0a8ad66d91ccad4aad6094f517a2182360c9aacf6a3dc323162cb6fd8cdffedb0fe038f55e85ffb5b6");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_HEX);
        }
    }

    @Test
    public void testAttr_ApacheBase64() {
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.MD5, "hvsmnRkNLIX24EaM7KQqIA==");

        signMap.put(ESA.SHA1, "00hq6RNueFa8QiEjhep5cJRHWAI=");
        signMap.put(ESA.SHA224, "foHr6eYEoMl/7w5M/nH5ug7LoTMyvelTrRxm5A==");
        signMap.put(ESA.SHA256, "wFNeS+K3n/2TKRMFQ2v4iTFOSj+uwF7P/Lt98xrZ5Ro=");
        signMap.put(ESA.SHA384, "hiVfosNuSzCWnq4X3DTHcsvr38WLWEA5AL6HYU6xo0uHgCY/JV615lypu7hkHMz+");
        signMap.put(ESA.SHA512, "9s3ioPgZMUzd5V/CJ9jX2uPSjMVWIioKitZtkcytSq1glPUXohgjYMmqz2o9wyMWLLb9jN/+2w/gOPVehf+1tg==");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_BASE64);
        }
    }

    // @Test
    public void testAttr_JavaBase64() { // same as Apache Base64
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.MD5, "hvsmnRkNLIX24EaM7KQqIA==");

        signMap.put(ESA.SHA1, "00hq6RNueFa8QiEjhep5cJRHWAI=");
        signMap.put(ESA.SHA224, "foHr6eYEoMl/7w5M/nH5ug7LoTMyvelTrRxm5A==");
        signMap.put(ESA.SHA256, "wFNeS+K3n/2TKRMFQ2v4iTFOSj+uwF7P/Lt98xrZ5Ro=");
        signMap.put(ESA.SHA384, "hiVfosNuSzCWnq4X3DTHcsvr38WLWEA5AL6HYU6xo0uHgCY/JV615lypu7hkHMz+");
        signMap.put(ESA.SHA512, "9s3ioPgZMUzd5V/CJ9jX2uPSjMVWIioKitZtkcytSq1glPUXohgjYMmqz2o9wyMWLLb9jN/+2w/gOPVehf+1tg==");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_JAVA_BASE64);
        }
    }

    @Test
    public void testAttr_RSA_Hex() {
        Map<ESA, String> signMap = new LinkedHashMap<>();
        //

        signMap.put(ESA.SHA1RSA,
                "a0f9625df2c897a2e6df61c6e9090b4bf0b0be7efc1b36f5f35463771df5e162665f0c7b24519574d4fe3f295c018be0c88bd9d2131241e59ce0de1e5420bcdad0c078ddae9d75aeb1ce5cb0a268fbb5426e9f17bb2c51231ac6b75cea51c412720e41f3a03ef0735d0add354c399283f082161e5486003e93d6507dd01546e162fb736aea4d5f98088bd97aa58b7dbd89181dbc73133a41cd50825d8d1b3e19cee1d64bf38474eec7942176a024eb4c819e416f8e2ca69ab92a88a4b9d3be8f3fcdb4afcfb53b9b35a9296fa645df4b4baf9f657f76ea4c554bd16e7e649003a87e22fd1f1195eff92c872f6d7787cd048d904013975a416f3c87934334c49f");
        signMap.put(ESA.SHA256RSA,
                "72f9f8684a6e74a06cd8bbd1b0cc9b7d66e8a87d20622c9d20df59842f3f97938a0f69e2163c7906fb9ade308558e3c7e22a14588681f94392802585d6e87b2768f8b27b21a7499c635d1aec55ad82a4ca1a761d734e345aae2a65f7efd8446af8f59d3dcc38b18757baab881c7a7ba7dcc5fa789a3089e63dbfc95ecd1b114a7e5fac082b7bdecf854f80085a1ecb8c9d7f2b3bd3b49fbeed0fc2fba6a160857cd45081d1e7b3179587bf3022b2e63ad58d602c437b8797120c38749079c849f2b6d81f2ed0cab75cedbc3f54d7daf227078ca8a97be3de6eb868c6ce5d73a44ad9fe8f954c4d1da5f9418a92455f7a8d04947d6c35f49b76702789ce593812");
        signMap.put(ESA.SHA512RSA,
                "a2d3255c6861eb53ecfb26b7b928bc03e3ff4122fa1f3cf69ccb6771a900e39d439513e8ff1a6687c92ac50c7f1c0e2d1198c254f739893f78acde6ddc5429db5ed944d988fb87823bc12af8e92da9812132b2bb40508552a69730a8d3065d2237f9efc4ac26271b54082e9796ba8db4f877144c1fab1d38a418eaa322edcce6cb7a4d5481092a57615d4a9ec900078e68cf964451f1a4a84e789a2935b13fac9d5d286184f910d5de6ee4df45b760b95214f0a0d0480d2c47159b5f2d64c2e1ca657002a3f1760cacd4c6daa9a255b4c261a0d5236612f46ebf2b6af8328b325050b2dc43e187a1eb7552dbfe1bb124f6e3694a5283518447b227ba2bc3e5bf");
        signMap.put(ESA.MD5RSA,
                "02383d5a37d8449364ad55b5ba56c1d4e93905eb54b3f2a01b5d81a917101b674aa9987595a6836b2896219bd9d7ea310347114015405df2e5ffbf6cefa69ea1fdb8ab86dc19b186370b9cc17be6add2e16916896914e37e121981faa4db77fda3324e2957553d125890a4a33cfa42e92f7fa8b053522a26424cf3a5f99b2676589fc82a96a716433564f8df602b0879d66970d05370058ba86d5caf61aee808ee1eeecc47c79c6ba8348c1765677e912d5e9f453d3ec6b7a96fddc38c010d9c08be8111457f540dfc8dfcffc3a929e9320a26ef5105cdd548b2c722740cfece532984150fde3e28474d1373c07674bb3d35c5b020f2d764d429e4b9d8322a41");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_HEX);
        }
    }

    @Test
    public void testAttr_RSA_Base64() {
        Map<ESA, String> signMap = new LinkedHashMap<>();
        //

        signMap.put(ESA.SHA1RSA,
                "oPliXfLIl6Lm32HG6QkLS/Cwvn78Gzb181Rjdx314WJmXwx7JFGVdNT+PylcAYvgyIvZ0hMSQeWc4N4eVCC82tDAeN2unXWusc5csKJo+7VCbp8XuyxRIxrGt1zqUcQScg5B86A+8HNdCt01TDmSg/CCFh5UhgA+k9ZQfdAVRuFi+3Nq6k1fmAiL2Xqli329iRgdvHMTOkHNUIJdjRs+Gc7h1kvzhHTux5QhdqAk60yBnkFvjiymmrkqiKS5076PP820r8+1O5s1qSlvpkXfS0uvn2V/dupMVUvRbn5kkAOofiL9HxGV7/kshy9td4fNBI2QQBOXWkFvPIeTQzTEnw==");
        signMap.put(ESA.SHA256RSA,
                "cvn4aEpudKBs2LvRsMybfWboqH0gYiydIN9ZhC8/l5OKD2niFjx5Bvua3jCFWOPH4ioUWIaB+UOSgCWF1uh7J2j4snshp0mcY10a7FWtgqTKGnYdc040Wq4qZffv2ERq+PWdPcw4sYdXuquIHHp7p9zF+niaMInmPb/JXs0bEUp+X6wIK3vez4VPgAhaHsuMnX8rO9O0n77tD8L7pqFghXzUUIHR57MXlYe/MCKy5jrVjWAsQ3uHlxIMOHSQechJ8rbYHy7Qyrdc7bw/VNfa8icHjKipe+Pebrhoxs5dc6RK2f6PlUxNHaX5QYqSRV96jQSUfWw19Jt2cCeJzlk4Eg==");
        signMap.put(ESA.SHA512RSA,
                "otMlXGhh61Ps+ya3uSi8A+P/QSL6Hzz2nMtncakA451DlRPo/xpmh8kqxQx/HA4tEZjCVPc5iT94rN5t3FQp217ZRNmI+4eCO8Eq+OktqYEhMrK7QFCFUqaXMKjTBl0iN/nvxKwmJxtUCC6XlrqNtPh3FEwfqx04pBjqoyLtzObLek1UgQkqV2FdSp7JAAeOaM+WRFHxpKhOeJopNbE/rJ1dKGGE+RDV3m7k30W3YLlSFPCg0EgNLEcVm18tZMLhymVwAqPxdgys1MbaqaJVtMJhoNUjZhL0br8ravgyizJQULLcQ+GHoet1Utv+G7Ek9uNpSlKDUYRHsie6K8Plvw==");
        signMap.put(ESA.MD5RSA,
                "Ajg9WjfYRJNkrVW1ulbB1Ok5BetUs/KgG12BqRcQG2dKqZh1laaDayiWIZvZ1+oxA0cRQBVAXfLl/79s76aeof24q4bcGbGGNwucwXvmrdLhaRaJaRTjfhIZgfqk23f9ozJOKVdVPRJYkKSjPPpC6S9/qLBTUiomQkzzpfmbJnZYn8gqlqcWQzVk+N9gKwh51mlw0FNwBYuobVyvYa7oCO4e7sxHx5xrqDSMF2VnfpEtXp9FPT7Gt6lv3cOMAQ2cCL6BEUV/VA38jfz/w6kp6TIKJu9RBc3VSLLHInQM/s5TKYQVD94+KEdNE3PAdnS7PTXFsCDy12TUKeS52DIqQQ==");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_BASE64);
        }
    }

    // @Test
    public void testAttr_DSA_Hex() {// the values of signature are different in each time
        Map<ESA, String> signMap = new LinkedHashMap<>();
        //

        signMap.put(ESA.SHA1DSA, "");
        signMap.put(ESA.SHA224DSA, "");
        signMap.put(ESA.SHA256DSA, "");

        // assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_HEX);
        }
    }

    // @Test
    public void testAttr_DSA_Base64() {// the values of signature are different in each time
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1DSA, "");
        signMap.put(ESA.SHA224DSA, "");
        signMap.put(ESA.SHA256DSA, "");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_BASE64);
        }
    }

    // @Test
    public void testAttr_EC_Hex() {// the values of signature are different in each time
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1EC, "");
        signMap.put(ESA.SHA256EC, "");
        signMap.put(ESA.SHA512EC, "");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_HEX);
        }
    }

    // @Test
    public void testAttr_EC_Base64() { // the values of signature are different in each time
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1EC, "");
        signMap.put(ESA.SHA256EC, "");
        signMap.put(ESA.SHA512EC, "");

        // assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_BASE64);
        }
    }

    @Test
    public void testAttr_MAC_ApacheHex() {
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1MAC, "a792a5638a59f36737c65df45bd75c1c359fdc6d");
        signMap.put(ESA.SHA256MAC, "34e09805763c8be15cd9e69210e563cb3725afce99eaabf71527204a91feaaa8");
        signMap.put(ESA.SHA512MAC, "833bafddb23ca963284e59fdcdbb8dd520e485b2676cc7e920152ff4d0772c819c682f2787e3951f4d0a312dc7caec6d8621e50bc0a56ce4bdfe3ff4d2cfbde3");
        signMap.put(ESA.MD5MAC, "7ed396f238465214e7d5ab37beecdf67");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_HEX);
        }
    }

    @Test
    public void testAttr_MAC_ApacheBase64() {
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1MAC, "p5KlY4pZ82c3xl30W9dcHDWf3G0=");
        signMap.put(ESA.SHA256MAC, "NOCYBXY8i+Fc2eaSEOVjyzclr86Z6qv3FScgSpH+qqg=");
        signMap.put(ESA.SHA512MAC, "gzuv3bI8qWMoTln9zbuN1SDkhbJnbMfpIBUv9NB3LIGcaC8nh+OVH00KMS3HyuxthiHlC8ClbOS9/j/00s+94w==");
        signMap.put(ESA.MD5MAC, "ftOW8jhGUhTn1as3vuzfZw==");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_APACHE_BASE64);
        }
    }

    // @Test
    public void testAttr_MAC_JavaBase64() { // same as Apache Base64
        Map<ESA, String> signMap = new LinkedHashMap<>();

        signMap.put(ESA.SHA1MAC, "p5KlY4pZ82c3xl30W9dcHDWf3G0=");
        signMap.put(ESA.SHA256MAC, "NOCYBXY8i+Fc2eaSEOVjyzclr86Z6qv3FScgSpH+qqg=");
        signMap.put(ESA.SHA512MAC, "gzuv3bI8qWMoTln9zbuN1SDkhbJnbMfpIBUv9NB3LIGcaC8nh+OVH00KMS3HyuxthiHlC8ClbOS9/j/00s+94w==");
        signMap.put(ESA.MD5MAC, "ftOW8jhGUhTn1as3vuzfZw==");

        assertUnTestESA(signMap);

        //
        for (Entry<ESA, String> entry : signMap.entrySet()) {
            assertAttrAlgCecretRunner(entry.getKey(), entry.getValue(), SignatureProcessor.SIGN_ENCODE_JAVA_BASE64);
        }
    }

    public static void main(String[] args) throws GeneralSecurityException {

        System.out.println(genPrivateKey("EC", 256));
        System.out.println();
        System.out.println(genPrivateKey("DSA", 512));
        System.out.println();
        System.out.println(genPrivateKey("RSA", 1014));
    }

    private static String genPrivateKey(String algorithm, int keysize) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(keysize);

        KeyPair keypair = kpg.generateKeyPair();

        // PublicKey publickey = keypair.getPublic();
        PrivateKey privatekey = keypair.getPrivate();

        String privatekeyValue = org.apache.commons.codec.binary.Base64.encodeBase64String(privatekey.getEncoded());

        StringBuilder pkBuilder = new StringBuilder(privatekeyValue);
        for (int i = 64; i < privatekeyValue.length(); i += 64) {
            if (pkBuilder.charAt(i) != '\n') {
                pkBuilder.insert(i, '\n');
            }
            i++;
        }

        pkBuilder.insert(0, String.format("-----BEGIN %s PRIVATE KEY-----\n", algorithm));
        pkBuilder.append(String.format("\n-----END %s PRIVATE KEY-----\n", algorithm));
        return pkBuilder.toString();

    }
}
