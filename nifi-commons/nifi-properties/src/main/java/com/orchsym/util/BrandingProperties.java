package com.orchsym.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class BrandingProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrandingProperties.class);

    private static final String FILE_NAME = "branding.properties";

    /**
     * Keys
     */
    public static final String KEY_RUNTIME_NAME = "orchsym.runtime.name";
    public static final String KEY_PRODUCT_NAME = "orchsym.product.name";
    public static final String KEY_SUPPORT_EMAIL = "orchsym.support.email";
    public static final String KEY_ROOT_GROUP_NAME = "orchsym.root.group.name";

    public static final String KEY_IMAGE_PREFIX = "orchsym.image.";

    /**
     * Default values
     */
    public static final String DEFAULT_RUNTIME_NAME = "Orchsym Runtime";
    public static final String DEFAULT_SHORT_RUNTIME_NAME = "Runtime";
    public static final String DEFAULT_PRODUCT_NAME = "Orchsym Studio";
    public static final String DEFAULT_SUPPORT_EMAIL = "orchsym-support@baishancloud.com";

    private static final Map<String, String> logoImages = new HashMap<>();
    static {
        logoImages.put("logo.ico", "nifi16.ico");
        logoImages.put("logo-error.png", "bg-error.png");
    }

    /**
     * 
     */
    private final Properties rawProperties = new Properties();
    private final File confDir;

    public BrandingProperties(final File configFolder) {
        confDir = configFolder;
        load();
    }

    public BrandingProperties() {
        confDir = OrchsymProperties.getConfDir();
        load();
    }

    private void load() {
        try (FileInputStream brandingStream = new FileInputStream(new File(confDir, FILE_NAME))) {
            rawProperties.load(brandingStream);
        } catch (IOException e) {
            LOGGER.error("Can't load " + FILE_NAME, e);
        }
    }

    @SuppressWarnings("rawtypes")
    public Set<String> getKeys() {
        Set<String> propertyNames = new HashSet<>();
        Enumeration e = rawProperties.propertyNames();
        for (; e.hasMoreElements();) {
            propertyNames.add((String) e.nextElement());
        }

        return propertyNames;
    }

    public int size() {
        return rawProperties.size();
    }

    public String getProperty(String key) {
        return rawProperties.getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        final String value = getProperty(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    public String getRuntimeName() {
        return getProperty(KEY_RUNTIME_NAME, DEFAULT_RUNTIME_NAME);
    }

    public String getProductName() {
        return getProperty(KEY_PRODUCT_NAME, DEFAULT_PRODUCT_NAME);
    }

    public String getSupportEmail() {
        return getProperty(KEY_SUPPORT_EMAIL, DEFAULT_SUPPORT_EMAIL);
    }

    public String getRootGroupName() {
        return getProperty(KEY_ROOT_GROUP_NAME, getProductName()); // product name is by default
    }

    public File getLogoImagePath(String key) {
        final String path = getProperty(key);
        if (path != null && !path.isEmpty()) {
            File imgFile = new File(path);
            if (!imgFile.isAbsolute()) {
                imgFile = new File(confDir, path);
            }
            if (imgFile.exists()) {
                return imgFile;
            }
        }
        return null;
    }

    public void syncWebImages(final File webImagesFolder) {
        getKeys().stream().filter(k -> k.startsWith(KEY_IMAGE_PREFIX)).forEach(k -> syncWebImage(webImagesFolder, k));
    }

    private void syncWebImage(final File webImagesFolder, String imageKey) {
        if (webImagesFolder == null || !webImagesFolder.exists()) {
            return;
        }
        final File srcImageFile = getLogoImagePath(imageKey);
        if (srcImageFile == null || !srcImageFile.exists()) {
            return;
        }

        final String imageName = srcImageFile.getName();
        String webImageName = logoImages.get(imageName);
        if (webImageName == null) { // same name
            webImageName = imageName;
        } // else //diff name

        File webImageFile = new File(webImagesFolder, webImageName);
        if (!webImageFile.exists()) {
            return;
        }

        final byte[] buffer = new byte[1024 * 4];
        int n;
        try (FileInputStream input = new FileInputStream(srcImageFile); FileOutputStream output = new FileOutputStream(webImageFile)) {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (IOException e) {
            // ignore to sync failure
        }
    }
}
