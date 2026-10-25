package twitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocialNetwork {

    private static final Pattern MENTION_PATTERN =
            Pattern.compile("(?<=^|[^A-Za-z0-9_-])@([A-Za-z0-9_-]+)");

    /**
     * Guess who might follow whom, from evidence found in tweets.
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        Map<String, Set<String>> followsGraph = new HashMap<>();

        for (Tweet tweet : tweets) {
            String author = tweet.getAuthor().toLowerCase();
            Set<String> mentioned = extractMentions(tweet.getText());
            mentioned.remove(author); // no self-follows

            if (!mentioned.isEmpty()) {
                followsGraph.computeIfAbsent(author, k -> new HashSet<>()).addAll(mentioned);
            }
        }

        return followsGraph;
    }

    /**
     * Find the people in a social network who have the greatest influence.
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        Map<String, Integer> followerCounts = new HashMap<>();

        for (String follower : followsGraph.keySet()) {
            followerCounts.putIfAbsent(follower, 0);
            for (String followee : followsGraph.get(follower)) {
                followerCounts.merge(followee, 1, Integer::sum);
            }
        }

        List<String> result = new ArrayList<>(followerCounts.keySet());
        result.sort((a, b) -> followerCounts.get(b) - followerCounts.get(a));

        return result;
    }

    private static Set<String> extractMentions(String text) {
        Set<String> mentions = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1).toLowerCase());
        }
        return mentions;
    }
}