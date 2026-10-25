/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the main program.
 *
 * Modified to run on a local sample of tweets instead of fetching a live
 * sample, since the original course sample server (ps1_tweets/tweetPoll.py)
 * is no longer reachable.
 */
public class Main {

    public static void main(String[] args) {
        final List<Tweet> tweets = Arrays.asList(
            new Tweet(1, "ernie", "hey @bert, nice job today!", Instant.parse("2026-01-01T10:00:00Z")),
            new Tweet(2, "bert", "thanks @ernie! also shoutout @elmo", Instant.parse("2026-01-01T10:05:00Z")),
            new Tweet(3, "elmo", "la la la, no mentions here", Instant.parse("2026-01-01T10:10:00Z")),
            new Tweet(4, "bigbird", "following @bert and @ernie and @elmo", Instant.parse("2026-01-01T10:15:00Z")),
            new Tweet(5, "cookie", "@elmo cookies for everyone!", Instant.parse("2026-01-01T10:20:00Z"))
        );

        System.err.println("using " + tweets.size() + " sample tweets");

        final Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);
        System.err.println("follows graph has " + followsGraph.size() + " nodes");

        final int count = 10;
        final List<String> influencers = SocialNetwork.influencers(followsGraph);
        for (String username : influencers.subList(0, Math.min(count, influencers.size()))) {
            System.out.println(username);
        }
    }

}