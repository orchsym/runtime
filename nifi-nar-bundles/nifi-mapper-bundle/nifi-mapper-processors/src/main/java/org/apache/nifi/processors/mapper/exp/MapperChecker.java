package org.apache.nifi.processors.mapper.exp;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author GU Guoqiang
 *
 */
public class MapperChecker {
    static {
        Runnable runable = new Runnable() {

            @Override
            public void run() {
                final String key = new String(Base64.getDecoder().decode("b3JjaHN5bS5saWMucGF0aA=="), //$NON-NLS-1$
                        StandardCharsets.UTF_8);
                try {
                    final FileInputStream stream = new FileInputStream(new File(System.getProperty(key)));
                    new Checker().check(stream);
                } catch (Throwable e) {
                    Runtime.getRuntime().exit(0);
                }
            }

        };
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(runable, 0, 2, TimeUnit.DAYS); // each 2 days
    }
}
