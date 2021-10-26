package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CrawlTask extends RecursiveAction {

    private final String url;
    private final Clock clock;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    private CrawlTask(String url,
                     Clock clock,
                     Instant deadline,
                     int maxDepth,
                     Map<String, Integer> counts,
                     Set<String> visitedUrls,
                     List<Pattern> ignoredUrls,
                     PageParserFactory parserFactory){
        this.url = url;
        this.clock = clock;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (!visitedUrls.add(url)) {
            return;
        }
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                synchronized (counts) {
                    counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
                }
            } else {
                synchronized (counts) {
                    counts.put(e.getKey(), e.getValue());
                }
            }
        }

        // !!Question: how can this step be done with lamda? (Example: CountWordsTask)
        List<CrawlTask> subTasks = new ArrayList<>();
        for (String link : result.getLinks()){
            CrawlTask.Builder builder = new CrawlTask.Builder()
                    .setUrl(link)
                    .setClock(clock)
                    .setDeadline(deadline)
                    .setMaxDepth(maxDepth - 1)
                    .setCounts(counts)
                    .setVisitedUrls(visitedUrls)
                    .setIgnoredUrls(ignoredUrls)
                    .setParserFactory(parserFactory);
            subTasks.add(builder.build());
        }

        invokeAll(subTasks);

    }

    public static class Builder{
        private String url = "";
        private Clock clock = null;
        private Instant deadline = null;
        private int maxDepth = 0;
        private Map<String, Integer> counts = null;
        private Set<String> visitedUrls = null;
        private List<Pattern> ignoredUrls = null;
        private PageParserFactory parserFactory = null;

        public Builder setUrl(String url){
            this.url = url;
            return this;
        }

        public Builder setClock(Clock clock){
            this.clock = clock;
            return this;
        }

        public Builder setDeadline(Instant deadline){
            this.deadline = deadline;
            return this;
        }

        public Builder setMaxDepth(int maxDepth){
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory){
            this.parserFactory = parserFactory;
            return this;
        }

        public CrawlTask build(){
            return new CrawlTask(this.url,this.clock,this.deadline,this.maxDepth,this.counts,this.visitedUrls,this.ignoredUrls,this.parserFactory);
        }
    }
}
