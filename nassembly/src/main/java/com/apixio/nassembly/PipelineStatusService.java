package com.apixio.nassembly;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.apixio.dao.utility.DaoServices;
import com.apixio.datasource.redis.RedisOps;

/**
 * A pipeline status service is a CRUD service for keeping track of pipeline status
 * - It uses a redis hashset to keep track of the information
 * - The key of the hashSet is either pds or pds,batch combination
 * - Each element of the hasSet is a representation of "PipelineStatus"
 *
 */

public class PipelineStatusService
{
    private RedisOps redisOps;
    private String   statusPrefix;
    private String   pdsId;

    /**
     * A Pipeline status class describes a pipeline status
     * - It has a number of tags such as datatype, jobs, etc
     * - It has the status
     * - It has the time the status was recorded
     */
    public static class PipelineStatus
    {
        public PipelineStatus(Set<String> tags, String status)
        {
            this.tags   = tags;
            this.status = status;
        }

        // not available for general use. Only in this class
        PipelineStatus(Set<String> tags, String status, long timeInMs)
        {
            this.tags     = tags;
            this.status   = status;
            this.timeInMs = timeInMs;
        }

        public Set<String> getTags()
        {
            return tags;
        }

        public String getStatus()
        {
            return status;
        }

        public long getTimeInMs()
        {
            return timeInMs;
        }

        private Set<String> tags;
        private String      status;
        private long        timeInMs;
    }

    public PipelineStatusService(DaoServices daoServices, String pdsId)
    {
        this.redisOps     = daoServices.getRedisOps();
        this.statusPrefix = daoServices.getRedisKeyPrefix() +  "pipeline-status-data2.0";
        this.pdsId        = pdsId;
    }

    public PipelineStatus readStatus(Optional<String> batchId, Set<String> tags)
    {
        String status = redisOps.hget(makeKey(batchId), combineTags(tags));

        return (status != null) ? new PipelineStatus(tags, getStatus(status), getTimeInMs(status)) : null;
    }

    public List<PipelineStatus> readAll(Optional<String> batchId)
    {
        Map<String, String> fieldToValue = redisOps.hgetAll(makeKey(batchId));

        return fieldToValue.keySet().stream().map(k ->
        {
            String status = fieldToValue.get(k);
            return new PipelineStatus(makeTags(k), getStatus(status), getTimeInMs(status));
        }).collect(Collectors.toList());
    }

    public void writeStatus(Optional<String> batchId, PipelineStatus pipelineStatus)
    {
        redisOps.hset(makeKey(batchId), combineTags(pipelineStatus.tags), combineStatus(pipelineStatus.status, System.currentTimeMillis()));
    }

    public void removeStatus(Optional<String> batchId, Set<String> tags)
    {
        redisOps.hdel(makeKey(batchId), combineTags(tags));
    }

    public void removeAll(Optional<String> batchId)
    {
        redisOps.del(makeKey(batchId));
    }

    private String makeKey(Optional<String> batchId)
    {
        if (batchId.isPresent())
            return String.join("-", statusPrefix, pdsId, batchId.get());
        else
            return String.join("-", statusPrefix, pdsId);
    }

    private String combineTags(Set<String> tags)
    {
        return tags.stream().sorted().collect(Collectors.joining("::"));
    }

    private Set<String> makeTags(String tag)
    {
        String[] tags = tag.split("::");

        return Arrays.stream(tags).collect(Collectors.toSet());
    }

    private String combineStatus(String status, long timeInMs)
    {
        return status + "::" + timeInMs;
    }

    private String getStatus(String st)
    {
        return st.split("::")[0];
    }

    private long getTimeInMs(String st)
    {
        return Long.valueOf(st.split("::")[1]);
    }
}