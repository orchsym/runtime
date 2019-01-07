package org.apache.nifi.web.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.bundle.BundleDetails;
import org.apache.nifi.nar.NarClassLoaders;

/**
 * @author GU Guoqiang
 *
 */
public class OrchsymVersionHelper {
    public static final OrchsymVersionHelper INSTANCE = new OrchsymVersionHelper();
    private final static String SNAPSHOT = "SNAPSHOT";

    private String nifiVersion;
    private String orchsymVersion;
    private String buildTag;
    private String buildRevision;
    private String buildBranch;
    private String buildTimestamp;

    private OrchsymVersionHelper() {
        nifiVersion = "2.2.0"; // default is fix one

        // nifi
        final Bundle frameworkBundle = NarClassLoaders.getInstance().getFrameworkBundle();
        if (frameworkBundle != null) {
            final BundleDetails frameworkDetails = frameworkBundle.getBundleDetails();

            nifiVersion = frameworkDetails.getCoordinate().getVersion();
            buildRevision = frameworkDetails.getBuildRevision();
            buildTag = frameworkDetails.getBuildTag();
            buildBranch = frameworkDetails.getBuildBranch();
            buildTimestamp = frameworkDetails.getBuildTimestamp();
        }

        try {
            final Class<?> versionClass = Class.forName("com.orchsym.core.ver.VersionUtil");
            final java.lang.reflect.Method getOrchsymVersionMethod = versionClass.getDeclaredMethod("getOrchsymVersion");
            getOrchsymVersionMethod.setAccessible(true);
            final Object oVersion = getOrchsymVersionMethod.invoke(null);
            if (oVersion != null) {
                orchsymVersion = oVersion.toString();
            }
        } catch (Exception e) {
            final String nifiBuildRevision = buildRevision;
            if (nifiBuildRevision != null) {
                final int lineIndex = nifiVersion.indexOf('-');
                if (lineIndex > 0 && !nifiVersion.endsWith(SNAPSHOT)) { // like 1.7.1-2.2.0
                    orchsymVersion = nifiVersion.substring(lineIndex); // 2.2.0
                }
            }
        }
        if (orchsymVersion == null) {
            orchsymVersion = nifiVersion;
        }

        if (buildRevision != null && (nifiVersion.contains(SNAPSHOT) || orchsymVersion.contains(SNAPSHOT)))
            orchsymVersion += '-' + buildRevision;

    }

    public String getNifiVersion() {
        return nifiVersion;
    }

    public String getOrchsymVersion() {
        return orchsymVersion;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public String getBuildRevision() {
        return buildRevision;
    }

    public String getBuildBranch() {
        return buildBranch;
    }

    public String getBuildTimestamp() {
        return buildTimestamp;
    }

    public Date getBuildTimestampDate() {
        return getBuildTimestampDate(buildTimestamp);
    }

    private Date getBuildTimestampDate(String buildTimestamp) {
        if (buildTimestamp != null && !buildTimestamp.isEmpty()) {
            try {
                SimpleDateFormat buildTimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Date buildTimestampDate = buildTimestampFormat.parse(buildTimestamp);
                return buildTimestampDate;
            } catch (ParseException parseEx) {
                return null;
            }
        } else {
            return null;
        }
    }
}
