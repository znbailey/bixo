package bixo.operations;

import java.util.ArrayList;

import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import bixo.cascading.NullContext;
import bixo.datum.BaseDatum;
import bixo.datum.GroupedUrlDatum;
import bixo.datum.ScoredUrlDatum;
import bixo.datum.UrlStatus;
import bixo.fetcher.RandomResponseHandler;
import bixo.fetcher.StringResponseHandler;
import bixo.fetcher.http.SimpleHttpFetcher;
import bixo.fetcher.simulation.TestWebServer;
import bixo.fetcher.util.FixedScoreGenerator;
import bixo.fetcher.util.ScoreGenerator;
import bixo.hadoop.FetchCounters;
import bixo.robots.RobotRulesParser;
import bixo.robots.SimpleRobotRulesParser;
import bixo.utils.ConfigUtils;
import bixo.utils.GroupingKey;
import cascading.flow.hadoop.HadoopFlowProcess;
import cascading.operation.BufferCall;
import cascading.operation.OperationCall;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryCollector;

public class FilterAndScoreByUrlAndRobotsTest {
    private static final String CRLF = "\r\n";

    private static class MatchBlockedByRobotsKey extends ArgumentMatcher<Tuple> {

        @Override
        public boolean matches(Object argument) {
            ScoredUrlDatum datum = new ScoredUrlDatum((Tuple)argument, BaseDatum.EMPTY_METADATA_FIELDS);
            return (datum.getGroupKey().equals(GroupingKey.BLOCKED_GROUPING_KEY));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUsingAllThreads() throws Exception {
        final int maxThreads = 10;
        
        SimpleHttpFetcher fetcher = new SimpleHttpFetcher(maxThreads, ConfigUtils.BIXO_TEST_AGENT);
        ScoreGenerator scorer = new FixedScoreGenerator(1.0);
        RobotRulesParser parser = new SimpleRobotRulesParser();
        FilterAndScoreByUrlAndRobots op = new FilterAndScoreByUrlAndRobots(fetcher, parser, scorer, BaseDatum.EMPTY_METADATA_FIELDS);
        
        HadoopFlowProcess fp = Mockito.mock(HadoopFlowProcess.class);
        Mockito.when(fp.getJobConf()).thenReturn(new JobConf());
        
        OperationCall<NullContext> oc = Mockito.mock(OperationCall.class);
        BufferCall<NullContext> bc = Mockito.mock(BufferCall.class);
        
        ArrayList<TupleEntry> iterValues = new ArrayList<TupleEntry>();
        iterValues.add(new TupleEntry(new GroupedUrlDatum("http://localhost:8089", 0, 0, UrlStatus.UNFETCHED, "http://localhost:8089", BaseDatum.EMPTY_METADATA_MAP).toTuple()));
        
        TupleEntryCollector collector = Mockito.mock(TupleEntryCollector.class);
        
        Mockito.when(bc.getGroup()).thenReturn(new TupleEntry(new Tuple("http://localhost:8089")));
        Mockito.when(bc.getArgumentsIterator()).thenReturn(iterValues.iterator());
        Mockito.when(bc.getOutputCollector()).thenReturn(collector);
        
        TestWebServer server = null;
        
        try {
            server = new TestWebServer(new RandomResponseHandler(100, 1000), 8089);
            op.prepare(fp, oc);

            for (int i = 0; i < maxThreads; i++) {
                op.operate(fp, bc);
            }
            
            // Give threads a chance to run, as otherwise we might call verify() before one of the ProcessRobotsTask
            // threads has been started.
            op.cleanup(fp, oc);
            
            Mockito.verify(fp, Mockito.times(maxThreads)).increment(FetchCounters.DOMAINS_PROCESSING, 1);
        } finally {
            server.stop();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBlockedRobots() throws Exception {
        final int maxThreads = 1;
        
        SimpleHttpFetcher fetcher = new SimpleHttpFetcher(maxThreads, ConfigUtils.BIXO_TEST_AGENT);
        ScoreGenerator scorer = new FixedScoreGenerator(1.0);
        RobotRulesParser parser = new SimpleRobotRulesParser();
        FilterAndScoreByUrlAndRobots op = new FilterAndScoreByUrlAndRobots(fetcher, parser, scorer, BaseDatum.EMPTY_METADATA_FIELDS);
        
        HadoopFlowProcess fp = Mockito.mock(HadoopFlowProcess.class);
        Mockito.when(fp.getJobConf()).thenReturn(new JobConf());
        
        OperationCall<NullContext> oc = Mockito.mock(OperationCall.class);
        BufferCall<NullContext> bc = Mockito.mock(BufferCall.class);
        
        ArrayList<TupleEntry> iterValues = new ArrayList<TupleEntry>();
        iterValues.add(new TupleEntry(new GroupedUrlDatum("http://localhost:8089", 0, 0, UrlStatus.UNFETCHED, "http://localhost:8089", BaseDatum.EMPTY_METADATA_MAP).toTuple()));
        
        TupleEntryCollector collector = Mockito.mock(TupleEntryCollector.class);
        
        Mockito.when(bc.getGroup()).thenReturn(new TupleEntry(new Tuple("http://localhost:8089")));
        Mockito.when(bc.getArgumentsIterator()).thenReturn(iterValues.iterator());
        Mockito.when(bc.getOutputCollector()).thenReturn(collector);
        
        TestWebServer server = null;
        
        try {
            final String disallowAllRobots = "User-agent: *" + CRLF
            + "Disallow: /";

            server = new TestWebServer(new StringResponseHandler("text/plain", disallowAllRobots), 8089);
            op.prepare(fp, oc);

            for (int i = 0; i < maxThreads; i++) {
                op.operate(fp, bc);
            }
            
            // Give threads a chance to run, as otherwise we might call verify() before one of the ProcessRobotsTask
            // threads has been started.
            op.cleanup(fp, oc);
            
            Mockito.verify(collector).add(Mockito.argThat(new MatchBlockedByRobotsKey()));
        } finally {
            server.stop();
        }
    }

}
