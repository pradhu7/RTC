package apixio.infraconfig.config;

public class CacheConfig {
    private Long cacheTtlSeconds = 120L;
    private Long cacheSizeInBytes = 20L * 1024L * 1024L;
    private Long cacheEntries = 200L;

    public Long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(Long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public Long getCacheSizeInBytes() {
        return cacheSizeInBytes;
    }

    public void setCacheSizeInBytes(Long cacheSizeInBytes) {
        this.cacheSizeInBytes = cacheSizeInBytes;
    }

    public Long getCacheEntries() {
        return cacheEntries;
    }

    public void setCacheEntries(Long cacheEntries) {
        this.cacheEntries = cacheEntries;
    }
}
