package org.elasticsearch.mapping;

import java.util.ArrayList;
import java.util.List;

public class SourceFetchContext {
    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();

    public List<String> getExcludes() {
        return excludes;
    }

    public List<String> getIncludes() {
        return includes;
    }
}