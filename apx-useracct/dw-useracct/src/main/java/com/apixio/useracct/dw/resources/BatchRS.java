package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.BatchLogic;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.buslog.ProjectLogic;
import com.apixio.useracct.buslog.ProjectLogic.PropertyBag;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.HccProject;
import com.apixio.useracct.entity.PatientDataSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal service.
 */
@Path("/batches")
@Produces("application/json")
public class BatchRS extends BaseRS {

    private PrivSysServices sysServices;
    private BatchLogic batchLogic;
    private PatientDataSetLogic pdsLogic;


    public BatchRS(ServiceConfiguration configuration, PrivSysServices sysServices) {
        super(configuration, sysServices);
        this.sysServices = sysServices;
        this.batchLogic = sysServices.getBatchLogic();
        this.pdsLogic = sysServices.getPatientDataSetLogic();
    }

    public static class BatchParams {
        public String description;
        public boolean bad;
        public double completenessRatio;
        public Map<String, Object> properties;
    }


    /**
     * Get Upload Batch By ID.
     *
     * @param batchId path parameter batch XUUID
     * @param pdsId query parameter 'pds' is optional for faster query while batches are prototyped as pds properties TODO: remove once batch entity is created
     * @param update  query parameter 'updateCompleteness' is true by default TODO: update currently does nothing
     * @return UploadBatch JSON Object: {id:"uploadBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, pipelineVersions:["#.#.#"]}
     */
    @GET
    @Path("/upload/{batchId}")
    public Response getUploadBatch(@Context HttpServletRequest request, @PathParam("batchId") final String batchId,
                                   @DefaultValue("") @QueryParam("pds") final String pdsId,
                                   @DefaultValue("true") @QueryParam("updateCompleteness") boolean update) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/{batchId}");
        logger.addParameter("batchId", batchId);
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("updateCompleteness", update);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                BatchLogic.UploadBatch batch;
                if (pdsId.isEmpty())  // TODO: once batch is a first class entity this won't be a special case.
                    batch = batchLogic.getUploadBatch(XUUID.fromString(batchId, BatchLogic.UploadBatch.OBJTYPE_PREFIX));
                else
                    batch = batchLogic.getUploadBatch(XUUID.fromString(batchId, BatchLogic.UploadBatch.OBJTYPE_PREFIX),
                            XUUID.fromString(pdsId, PatientDataSet.OBJTYPE));
                if (batch == null)
                    return notFound("UploadBatch", batchId);
                return ok(uploadBatchToMap(batch));
            }
        });
    }

    /**
     * Get Event Extract Batch By ID.
     *
     * @param batchId path parameter batch XUUID
     * @param pdsId query parameter 'pds' is optional for faster query while batches are prototyped as pds properties TODO: remove once batch entity is created
     * @param update query parameter 'updateCompleteness' is true by default TODO: update currently does nothing
     * @return Upload Batch JSON Object: {id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"#.#.#"}
     */
    @GET
    @Path("/extract/{batchId}")
    public Response getExtractBatch(@Context HttpServletRequest request, @PathParam("batchId") final String batchId,
                                    @DefaultValue("") @QueryParam("pds") final String pdsId,
                                    @DefaultValue("true") @QueryParam("updateCompleteness") boolean update) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/extract/{batchId}");
        logger.addParameter("batchId", batchId);
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("updateCompleteness", update);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                BatchLogic.EventExtractBatch batch;
                if (pdsId.isEmpty())  // TODO: once batch is a first class entity this won't be a special case.
                    batch = batchLogic.getExtractBatch(XUUID.fromString(batchId, BatchLogic.EventExtractBatch.OBJTYPE_PREFIX));
                else
                    batch = batchLogic.getExtractBatch(XUUID.fromString(batchId, BatchLogic.EventExtractBatch.OBJTYPE_PREFIX),
                            XUUID.fromString(pdsId, PatientDataSet.OBJTYPE));
                if (batch == null)
                    return notFound("ExtractBatch", batchId);
                return ok(extractBatchToMap(batch));
            }
        });
    }

    /**
     * Get available upload batches for a patient data set.
     *
     * @param pdsId path parameter patient data set XUUID
     * @return List of Upload Batch JSON Objects: [{id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"#.#.#"},...]
     */
    @GET
    @Path("/upload/pds/{pdsId}")
    public Response getAvailableUploadBatchesForPDS(@Context HttpServletRequest request,
                                                    @PathParam("pdsId") final String pdsId,
                                                    @DefaultValue("false") @QueryParam("includeBad") final boolean includeBad,
                                                    @DefaultValue("0") @QueryParam("start") final long start,
                                                    @DefaultValue("0") @QueryParam("end") final long end) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/pds/{pdsId}");
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("includeBad", includeBad);
        logger.addParameter("start", start);
        logger.addParameter("end", end);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {

                Date startDate = start == 0L ? null : new Date(start);
                Date endDate = end == 0L ? null : new Date(end);

                List<Map<String, Object>> filteredBatches = new ArrayList<>();
                List<BatchLogic.UploadBatch> batches = batchLogic.getUploadBatches(getXuuid(pdsId, PatientDataSet.OBJTYPE),
                        !includeBad); // filterBad = !includeBad
                for (BatchLogic.UploadBatch b : batches) {
                    if ((startDate == null || startDate.before(b.getStartDate())) && (endDate == null || endDate.after(b.getCloseDate())))
                        filteredBatches.add(uploadBatchToMap(b));
                }

                return ok(filteredBatches);
            }
        });
    }

    /**
     * Get available upload batches for a patient data set.
     *
     * @param pdsId path parameter patient data set XUUID
     * @return List of Upload Batch JSON Objects: [{id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"#.#.#"},...]
     */
    @GET
    @Path("/upload/bad/pds/{pdsId}/{category}")
    public Response getBadUploadBatchesForPDS(@Context HttpServletRequest request,
                                                                @PathParam("pdsId") final String pdsId,
                                                                @PathParam("category") final String category,
                                                                @DefaultValue("0") @QueryParam("start") final long start,
                                                                @DefaultValue("0") @QueryParam("end") final long end) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/bad/pds/{pdsId}");
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("category", category);
        logger.addParameter("start", start);
        logger.addParameter("end", end);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if(end<start) {
                    return badRequest("End can not be less than start to start");
                }

                XUUID pdsID =  PatientDataSetLogic.normalizePdsIdToXuuidFormat(pdsId);

                List<Map<String, Object>> filteredBatches = new ArrayList<>();
                List<BatchLogic.UploadBatch> batches = batchLogic.getBadUploadBatches(pdsID, start, end);

                for (BatchLogic.UploadBatch b : batches) {
                    filteredBatches.add(uploadBatchToMap(b));
                }

                return ok(filteredBatches);
            }
        });
    }

    @GET
    @Path("/upload/lastmergedowntime/{pdsId}/{category}")
    public Response getLastMergeDownTime(@Context HttpServletRequest request,
                                                                @PathParam("pdsId") final String pdsId,
                                                                @PathParam("category") final String category) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/lastmergedowntime/{pdsId}/{category}");
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("category", category);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                XUUID pdsID =  PatientDataSetLogic.normalizePdsIdToXuuidFormat(pdsId);
                //Start With the last merge down time, if not set this should be 0
                long start = getLastPatientAssemblyMergeDownDateTime(pdsID, category);
                return ok(start);
            }
        });
    }


    /**
     * Get available upload batches for a patient data set.
     *
     * @param pdsId path parameter patient data set XUUID
     * @return List of Upload Batch JSON Objects: [{id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"#.#.#"},...]
     */
    @GET
    @Path("/upload/bad/pds/lastmergedown/{pdsId}/{category}")
    public Response getBadUploadBatchesForPDSSinceLastMergeDown(@Context HttpServletRequest request,
                                                    @PathParam("pdsId") final String pdsId,
                                                    @PathParam("category") final String category) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/bad/pds/lastmergedown/{pdsId}/{category}");
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("category", category);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                XUUID pdsID =  PatientDataSetLogic.normalizePdsIdToXuuidFormat(pdsId);

                //Start With the last merge down time, if not set this should be 0
                long start = getLastPatientAssemblyMergeDownDateTime(pdsID, category);

                //The end time is the latest available bad batch.
                long end = Long.MAX_VALUE;

                List<Map<String, Object>> filteredBatches = new ArrayList<>();
                List<BatchLogic.UploadBatch> batches = batchLogic.getBadUploadBatches(pdsID, start, end);

                for (BatchLogic.UploadBatch b : batches) {
                    filteredBatches.add(uploadBatchToMap(b));
                }

                return ok(filteredBatches);
            }
        });
    }

    /**
     * Get available event extract batches for a patient data set.
     *
     * @param pdsId path parameter patient data set XUUID
     * @return List of Extract Batch JSON Objects: [{id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"3.4.5"},...]
     */
    @GET
    @Path("/extract/pds/{pdsId}")
    public Response getAvailableExtractBatchesForPDS(@Context HttpServletRequest request,
                                                     @PathParam("pdsId") final String pdsId,
                                                     @DefaultValue("false") @QueryParam("includeBad") final boolean includeBad,
                                                     @DefaultValue("0") @QueryParam("start") final long start,
                                                     @DefaultValue("0") @QueryParam("end") final long end,
                                                     @DefaultValue("") @QueryParam("sourceSystem") final String sourceSystem,
                                                     @DefaultValue("") @QueryParam("modelVersion") final String modelVersion) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/pds/{pdsId}");
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("includeBad", includeBad);
        logger.addParameter("start", start);
        logger.addParameter("end", end);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {

                Date startDate = start == 0L ? null : new Date(start);
                Date endDate = end == 0L ? null : new Date(end);

                List<Map<String, Object>> filteredBatches = new ArrayList<>();
                List<BatchLogic.EventExtractBatch> batches = batchLogic.getExtractBatches(XUUID.fromString(pdsId, PatientDataSet.OBJTYPE),
                        !includeBad); // filterBad = !includeBad
                for (BatchLogic.EventExtractBatch b : batches) {
                    if ((startDate == null || startDate.before(b.getStartDate())) && (endDate == null || endDate.after(b.getCloseDate()))
                            && compareFilterValues(sourceSystem, b.getSourceSystem())
                            && compareFilterValues(modelVersion, b.getModelVersion()))
                        filteredBatches.add(extractBatchToMap(b));
                }

                return ok(filteredBatches);
            }
        });
    }

    /**
     * Get available event extract batches for a project.
     *
     * @param projectId path parameter project XUUID
     * @return List of Extract Batch JSON Objects: [{id:"extractBatchId", name:"pipelineBatchName", desc:"desc", pds:"pdsId", startDate:timestamp, closeDate:timestamp, bad:true/false, completenessRatio:#.#, parentUploadBatch:"uploadBatchId", sourceSystem:"sourceSystem", healthPlan:"healthPlan", modelVersion:"version", pipelineVersion:"3.4.5"},...]
     */
    @GET
    @Path("/extract/project/{projectId}")
    public Response getAvailableExtractBatchesForProject(@Context HttpServletRequest request,
                                                         @PathParam("projectId") final String projectId) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/extract/project/{projectId}");
        logger.addParameter("projectId", projectId);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                ProjectLogic progic = sysServices.getProjectLogic();

                HccProject project = progic.getHccProjectByID(projectId); // was going to use generic project, but not guaranteed to have dosStart
                Date dosStart = project.getDosStart();
                Map<String, Object> properties = progic.getProjectProperties(PropertyBag.GENERIC, project);
                String sourceSystem = (String) properties.get("sourcesystem");
                String modelVersion = (String) properties.get("extractmodel");

                List<Map<String,Object>> filteredBatches = new ArrayList<>();
                List<BatchLogic.EventExtractBatch> batches = batchLogic.getExtractBatches(project.getPatientDataSetID()); // bad batches are not included
                for (BatchLogic.EventExtractBatch b : batches) {
                    if ((dosStart == null || dosStart.before(b.getStartDate()))
                            && compareFilterValues(sourceSystem, b.getSourceSystem())
                            && compareFilterValues(modelVersion, b.getModelVersion()))
                        filteredBatches.add(extractBatchToMap(b));
                }

                return ok(filteredBatches);
            }
        });
    }

    /**
     * @param a
     * @param b
     * @return true if either a or b is not set or they have matching values, false if they are both set and do not match
     */
    private boolean compareFilterValues(String a, String b) {
        return a == null || b == null || a.isEmpty() || b.isEmpty() || a.equalsIgnoreCase(b);
    }

    /**
     * Update existing Upload Batch.
     *
     * @param batchId path parameter batch XUUID
     * @param pdsId query parameter 'pds' is optional for faster query while batches are prototyped as pds properties TODO: remove once batch entity is created
     * @param params mutable batch attributes JSON: {desc:"updated desc", bad:true/false, completenessRatio:#.#}
     */
    @PUT
    @Path("/upload/{batchId}")
    public Response updateUploadBatch(@Context HttpServletRequest request, @PathParam("batchId") final String batchId,
                                      @DefaultValue("") @QueryParam("pds") final String pdsId, final BatchParams params) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/upload/{batchId}");
        logger.addParameter("batchId", batchId);
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("bad", params.bad);
        logger.addParameter("completenessRatio", params.completenessRatio);
        logger.addParameter("description", params.description);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (params == null)
                    return badRequest("No parameters to update provided.");

                BatchLogic.UploadBatch batch;
                if (pdsId.isEmpty())  // TODO: once batch is a first class entity this won't be a special case.
                    batch = batchLogic.getUploadBatch(getXuuid(batchId, BatchLogic.UploadBatch.OBJTYPE_PREFIX));
                else
                    batch = batchLogic.getUploadBatch(getXuuid(batchId, BatchLogic.UploadBatch.OBJTYPE_PREFIX),
                            getXuuid(pdsId, PatientDataSet.OBJTYPE));

                batch.setBad(params.bad);
                batch.setDesc(params.description);
                batch.setCompletenessRatio(params.completenessRatio);
                batch.setProperties(params.properties);
                batchLogic.updateBatch(batch);

                return ok();
            }
        });
    }

    /**
     * Update existing Extract Batch.
     *
     * @param batchId path parameter batch XUUID
     * @param pdsId query parameter 'pds' is optional for faster query while batches are prototyped as pds properties TODO: remove once batch entity is created
     * @param params mutable batch attributes JSON: {desc:"updated desc", bad:true/false, completenessRatio:#.#}
     */
    @PUT
    @Path("/extract/{batchId}")
    public Response updateExtractBatch(@Context HttpServletRequest request, @PathParam("batchId") final String batchId,
                                      @DefaultValue("") @QueryParam("pds") final String pdsId, final BatchParams params) {
        final ApiLogger logger = super.createApiLogger(request, "/batches/extract/{batchId}");
        logger.addParameter("batchId", batchId);
        logger.addParameter("pdsId", pdsId);
        logger.addParameter("bad", params.bad);
        logger.addParameter("completenessRatio", params.completenessRatio);
        logger.addParameter("description", params.description);

        return super.restWrap(new RestWrap(logger) {
            @Override
            public Response operation() throws Exception {
                if (params == null)
                    return badRequest("No parameters to update provided.");

                BatchLogic.EventExtractBatch batch;
                if (pdsId.isEmpty())  // TODO: once batch is a first class entity this won't be a special case.
                    batch = batchLogic.getExtractBatch(XUUID.fromString(batchId, BatchLogic.EventExtractBatch.OBJTYPE_PREFIX));
                else
                    batch = batchLogic.getExtractBatch(XUUID.fromString(batchId, BatchLogic.EventExtractBatch.OBJTYPE_PREFIX),
                            XUUID.fromString(pdsId, PatientDataSet.OBJTYPE));

                batch.setBad(params.bad);
                batch.setDesc(params.description);
                batch.setCompletenessRatio(params.completenessRatio);
                batch.setProperties(params.properties);
                batchLogic.updateBatch(batch);

                return ok();
            }
        });
    }

    private Map<String, Object> batchToMap(BatchLogic.Batch batch) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", batch.getName());
        map.put("id", batch.getId());
        map.put("bad", batch.getBad());
        map.put("pds", batch.getPds());
        map.put("description", batch.getDesc());
        map.put("properties", batch.getProperties());
        map.put("startDate", batch.getStartDate());
        map.put("closeDate", batch.getCloseDate());
//        map.put("completenessRatio", batch.getCompletenessRatio()); // TODO: add completeness
        return map;
    }
    private Map<String, Object> uploadBatchToMap(BatchLogic.UploadBatch batch) {
        Map<String, Object> map = batchToMap(batch);
        map.put("pipelineVersions", batch.getPipelineVersions());
        return map;
    }
    private Map<String, Object> extractBatchToMap(BatchLogic.EventExtractBatch batch) {
        Map<String, Object> map = batchToMap(batch);
        map.put("modelVersion", batch.getModelVersion());
        map.put("parentBatch", batch.getParentUploadBatch());
        map.put("pipelineVersion", batch.getPipelineVersion());
        map.put("sourceSystem", batch.getSourceSystem());
        return map;
    }

    private long getLastPatientAssemblyMergeDownDateTime(XUUID orgId, String category)
    {
        long lastMergeDownTime = 0;
        try
        {
            lastMergeDownTime =  Long.valueOf(getPropertyFromPatientDataSet(PatientDataSetConstants.LAST_PATIENTASSEMBLY_MERGE_DOWN_DATETIME + "_" + category, orgId));
        }
        catch (Exception e)
        {
            //ignore
        }
        return lastMergeDownTime;
    }

    private String getPropertyFromPatientDataSet(String propertyName, XUUID pdsID)
    {
        PatientDataSet      customer    = pdsLogic.getPatientDataSetByID(pdsID);
        Map<String, Object> nameToValue = customer != null ? pdsLogic.getPatientDataSetProperties(customer) : null;
        String              value       = nameToValue != null ? (String) nameToValue.get(propertyName) : null;
        return value;
    }

    private XUUID getXuuid(String idStr, String type)
    {
        XUUID xuuid = null;

        try
        {
            xuuid = XUUID.fromString(idStr, type);
        }
        catch (IllegalArgumentException iae)
        {
            badRequest(iae.getMessage());
        }

        return xuuid;
    }

}
