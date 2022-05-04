package com.apixio.accessors;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.SignalCombinationParams;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxExecutor;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.builtin.ApxQueryDataUriManager;
import com.apixio.sdk.util.FxEvalParser;

public class SignalGroupsAccessor extends BaseAccessor
{

    protected FxExecutor executor;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        super.setEnvironment(env);

        executor = fxEnv.getFxExecutor();
    }

    /**
     * Signature:  com.apixio.model.patient.Patient patient(String uuid)
     */
    @Override
    public String getID()
    {
        return "signalGroup";
    }


    // want:  signalGroup(mcid1, mcid2, mcid3, ...)
    // but need a patientuuid also in order to form dataURIs, so
    // need:  signalGroup(patientuuid, mcid1, mcid2, mcid3, ...)
    // where patientuuid MUST come from the request somehow.
    //
    // signalGroup(request('patientuuid'), 'B_slim', 'B_anno', 'B_har2', 'B_ssusp')

    @Override
    public Object eval(AccessorContext req, List<Object> args) throws Exception
    {
        List<Signal> allSignals = new ArrayList<>();

        if (args.size() == 0)
            throw new IllegalArgumentException("Missing patientuuid.  Usage: signalGroup(patientuuid, mcid, ...)");

        String uuid = "'" + args.remove(0) + "'";  // add surrounding 's as this string is parsed via FxEvalParser

        for (Object arg: args) {
            try {
                URI          dataURI = makeUriFromMCID(uuid, (String) arg);
                List<Object> objs    = this.executor.restoreOutput(dataURI, "apixio.Signal");

                if (objs != null) {
                    logger.debug("SignalGroupsAccessor::Signal count:" + objs.size() + ":" + arg);
                }
                for (Object obj: objs) {
                    if (obj instanceof Signal) {
                        allSignals.add((Signal) obj);
                    }
                }
            } catch (IllegalStateException ex) {
                // We do not error out in the case that a specific Data URI does not exist, because there 
                // are valid cases in Cerebro where Fx's are skipped if there are no data
                logger.info("SignalGroupsAccessor::Failed to restore DataURI %s", (String) arg);
            }
        }
        SignalCombinationParamsImpl scpi = new SignalCombinationParamsImpl();
        scpi.setSignals(allSignals);
        logger.debug("SignalGroupsAccessor::Signal count:" + allSignals.size() + ":TOTAL");
        return scpi;
    }

    private URI makeUriFromMCID(String patientuuid, String mcid) throws Exception
    {
        ApxQueryDataUriManager dum = new ApxQueryDataUriManager();

        dum.setEnvironment(fxEnv);
        dum.setPrimaryKeyArgs(FxEvalParser.parse("'" + mcid + "'," + patientuuid),
                              Arrays.asList("algo", "patientuuid"));

        return dum.makeDataURI(new DummyRequest());
    }

    private class SignalCombinationParamsImpl implements SignalCombinationParams {
        List<Signal> signals = null;

		@Override
		public XUUID getDocumentId() {
			return null;
		}

		@Override
		public Map<String, String> getEventTransferMapping() {
			return null;
		}

		@Override
		public XUUID getPatientId() {
			return null;
		}

		@Override
		public List<Signal> getSignalList() {
			return signals;
		}

        public void setSignals(List<Signal> newsignals) {
            this.signals = newsignals;
            //.stream().map(a -> (Signal) a).collect(Collectors.toList());
        }
    }

    private static class DummyRequest implements FxRequest
    {
        DummyRequest()
        {
        }

        public String getAttribute(String id)
        {
            return null;
        }

        public Map<String, String> getAttributes()
        {
            return null;
        }

        public void setAttribute(String name, String value)
        {
        }
    }

}
