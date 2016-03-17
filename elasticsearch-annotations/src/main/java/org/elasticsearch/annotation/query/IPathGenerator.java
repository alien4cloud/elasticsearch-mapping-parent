package org.elasticsearch.annotation.query;

/**
 * Created by lucboutier on 17/03/2016.
 */
public interface IPathGenerator {
    /**
     * Generate paths to be used from the one set on the annotation.
     * 
     * @param annotationPaths The paths configured on the annotation.
     * @return A String array to override the annotationPaths
     */
    String[] getPaths(String[] annotationPaths);

    class DEFAULT implements IPathGenerator {
        @Override
        public String[] getPaths(String[] annotationPaths) {
            return annotationPaths;
        }
    }
}
