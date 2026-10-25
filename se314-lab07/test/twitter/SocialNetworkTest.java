/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy
     *
     * guessFollowsGraph():
     *   partition on number of tweets: 0, 1, >1
     *   partition on number of @-mentions per tweet: 0, 1, >1
     *   partition on mentions from same author across multiple tweets: present/absent
     *   (we only assert what the spec guarantees: author follows whoever they
     *    @-mention; we do not assert anything about usernames that are never
     *    authors or mentions, and we compare usernames case-insensitively)
     *
     * influencers():
     *   partition on graph size: empty, 1 user, >1 user
     *   partition on follower counts: all zero, single clear max, tie
     *   (we only assert relative ordering by follower count; for ties we
     *    assert both appear and both come after/before the correct
     *    non-tied users, without asserting their relative order to each other)
     */

    private static final Instant testTimestamp = Instant.parse("2016-02-17T10:00:00Z");

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    @Test
    public void testGuessFollowsGraphNoMentions() {
        Tweet tweet = new Tweet(1, "alice", "just having coffee this morning", testTimestamp);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        // alice shouldn't be shown as following anyone
        Set<String> alicesFollows = getFollowsCaseInsensitive(followsGraph, "alice");
        assertTrue("expected no one followed", alicesFollows.isEmpty());
    }

    @Test
    public void testGuessFollowsGraphSingleMention() {
        Tweet tweet = new Tweet(2, "ernie", "hey @bert, nice job!", testTimestamp);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        Set<String> erniesFollows = getFollowsCaseInsensitive(followsGraph, "ernie");
        assertTrue("expected ernie to follow bert", containsCaseInsensitive(erniesFollows, "bert"));
    }

    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Tweet tweet = new Tweet(3, "ernie", "thanks @bert and @elmo for the help!", testTimestamp);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        Set<String> erniesFollows = getFollowsCaseInsensitive(followsGraph, "ernie");
        assertTrue("expected ernie to follow bert", containsCaseInsensitive(erniesFollows, "bert"));
        assertTrue("expected ernie to follow elmo", containsCaseInsensitive(erniesFollows, "elmo"));
    }

    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        Tweet tweet1 = new Tweet(4, "ernie", "hi @bert", testTimestamp);
        Tweet tweet2 = new Tweet(5, "ernie", "hi @elmo", testTimestamp);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1, tweet2));

        Set<String> erniesFollows = getFollowsCaseInsensitive(followsGraph, "ernie");
        assertTrue("expected ernie to follow bert", containsCaseInsensitive(erniesFollows, "bert"));
        assertTrue("expected ernie to follow elmo", containsCaseInsensitive(erniesFollows, "elmo"));
    }

    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("expected only alice in the list", 1, influencers.size());
        assertTrue("expected alice in the list", containsCaseInsensitive(new HashSet<>(influencers), "alice"));
    }

    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>(Arrays.asList("bob")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // bob has 1 follower (alice), alice has 0 -> bob must come before alice
        int bobIndex = indexOfCaseInsensitive(influencers, "bob");
        int aliceIndex = indexOfCaseInsensitive(influencers, "alice");
        assertTrue("expected both users present", bobIndex >= 0 && aliceIndex >= 0);
        assertTrue("expected bob (more followers) ranked above alice", bobIndex < aliceIndex);
    }

    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        // carol followed by alice and bob (2 followers)
        // dan followed by alice only (1 follower)
        followsGraph.put("alice", new HashSet<>(Arrays.asList("carol", "dan")));
        followsGraph.put("bob", new HashSet<>(Arrays.asList("carol")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        int carolIndex = indexOfCaseInsensitive(influencers, "carol");
        int danIndex = indexOfCaseInsensitive(influencers, "dan");
        assertTrue("expected carol and dan present", carolIndex >= 0 && danIndex >= 0);
        assertTrue("expected carol (2 followers) ranked above dan (1 follower)", carolIndex < danIndex);
    }

    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        // both bob and carol have exactly 1 follower (alice); dan has 0
        followsGraph.put("alice", new HashSet<>(Arrays.asList("bob", "carol")));
        followsGraph.put("dan", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        int bobIndex = indexOfCaseInsensitive(influencers, "bob");
        int carolIndex = indexOfCaseInsensitive(influencers, "carol");
        int danIndex = indexOfCaseInsensitive(influencers, "dan");

        assertTrue("expected all three present", bobIndex >= 0 && carolIndex >= 0 && danIndex >= 0);
        // don't assert bob-vs-carol order (tie is underdetermined), just that
        // both outrank dan, who has fewer followers
        assertTrue("expected bob ranked above dan", bobIndex < danIndex);
        assertTrue("expected carol ranked above dan", carolIndex < danIndex);
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     * 
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

    // ---- helper methods (test-class-only, per the warning above) ----

    private static Set<String> getFollowsCaseInsensitive(Map<String, Set<String>> graph, String username) {
        for (String key : graph.keySet()) {
            if (key.equalsIgnoreCase(username)) {
                return graph.get(key);
            }
        }
        return new HashSet<>();
    }

    private static boolean containsCaseInsensitive(Set<String> set, String value) {
        for (String s : set) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfCaseInsensitive(List<String> list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }
}