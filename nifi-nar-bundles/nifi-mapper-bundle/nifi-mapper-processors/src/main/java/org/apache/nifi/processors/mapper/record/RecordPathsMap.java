package org.apache.nifi.processors.mapper.record;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.record.path.RecordPath;
import org.apache.nifi.record.path.util.RecordPathCache;

/**
 * compile the record path for the expressions.
 * 
 * @author GU Guoqiang
 */
public class RecordPathsMap extends RecordPathCache {
    private final Set<String> paths;

    private MapperTable mapperTable;

    public RecordPathsMap(final MapperTable mapperTable) {
        this(mapperTable.getExpressions().size());
        this.mapperTable = mapperTable;

        this.mapperTable.getExpressions().forEach(e -> {
            getCompiled(e.getPath()); // init paths
        });
    }

    public RecordPathsMap(final int cacheSize) {
        super(cacheSize);
        this.paths = new HashSet<>(cacheSize);
    }

    public Set<String> getPathsSet() {
        return Collections.unmodifiableSet(paths);
    }

    public RecordPath getCompiled(final String path) {
        RecordPath compiled = super.getCompiled(path);

        if (!path.contains(path)) {
            paths.add(path);
        }
        return compiled;
    }

}
