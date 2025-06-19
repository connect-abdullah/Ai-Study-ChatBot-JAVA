import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;


public class StudyChatbot {

    // ==============================================
    // SECTION 1: Configuration & Constants
    // ==============================================
    private static final String API_KEY = "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";
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "deepseek/deepseek-r1:free";
    private static final String SYSTEM_PROMPT = "You are an AI Study Guide Chatbot designed to help students learn effectively.\n\n" +
            "When responding to a question:\n" +
            "1. Start with a brief, friendly introduction.\n" +
            "2. Break down complex topics into simple, easy-to-understand explanations.\n" +
            "3. Use relevant examples and analogies when helpful.\n" +
            "4. End with a summary of key takeaways.\n\n" +
            "Format your responses for maximum clarity:\n" +
            "• Use clear headings like ## Title ##.\n" +
            "• Do not use markdown for bold text like **word**; display it as simple text.\n" +
            "• Use '→' for bullet points.\n" +
            "• Use numbered steps for processes or sequences.\n" +
            "• Format code examples in ```language code``` blocks.\n" +
            "• Ensure there is double spacing between sections for readability.\n\n" +
            "Keep your language simple, encouraging, and student-friendly!";

    // A comprehensive set of stop words to filter out common, non-essential words.
    private static final Set<String> GENERAL_STOP_WORDS = new HashSet<>(Arrays.asList(
            "what", "is", "the", "of", "and", "to", "in", "a", "for", "does", "how", "can", "i",
            "are", "you", "your", "that", "this", "with", "from", "by", "on", "at", "as", "an",
            "be", "have", "has", "had", "do", "did", "will", "would", "shall", "should",
            "may", "might", "must", "could", "if", "then", "else", "when", "where", "why", "who",
            "which", "whom", "whose", "but", "or", "nor", "yet", "so", "because", "although",
            "though", "while", "since", "until", "unless", "whether", "both", "either", "neither",
            "not", "no", "yes", "very", "much", "many", "more", "most", "less", "least", "few",
            "about", "tell", "me", "steps", "explain", "describe", "process", "procedure", "way", "method",
            "give", "an", "example", "instances", "cases", "samples", "illustrations"
    ));

    // A single cache file for all question responses.
    private static final String CACHE_FILE = "responses_cache.txt";
    private static final double SIMILARITY_THRESHOLD = 0.72; // Min score (0.0-1.0) to be considered a match.

    // Common question patterns for improved semantic intent detection.
    private static final String[] WHAT_IS_PATTERNS = {"what is", "what are", "define", "definition of", "meaning of", "tell me about", "explain", "describe", "information on"};
    private static final String[] HOW_TO_PATTERNS = {"how to", "how do i", "how can i", "steps to", "way to", "method to", "process of", "procedure for"};
    private static final String[] WHY_PATTERNS = {"why does", "why is", "why are", "reason for", "cause of", "explanation for", "purpose of"};
    private static final String[] COMPARISON_PATTERNS = {"difference between", "compare", "comparison", "versus", "vs", "distinguish between", "contrast"};
    private static final String[] EXAMPLE_PATTERNS = {"examples of", "example of", "instances of", "cases of", "samples of", "illustrations of", "give me an example"};

    private static final Scanner scanner = new Scanner(System.in);

    // ==============================================
    // SECTION 2: Main Program & User Interaction
    // ==============================================

    public static void main(String[] args) {
        printWelcomeBanner();

        while (true) {
            System.out.println("\n" + repeatString("-", 50));
            System.out.println("📝 Ask a question, or type 'exit' to quit.");
            System.out.print("➜ ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit")) {
                printGoodbyeBanner();
                break;
            }

            if (!userInput.isEmpty()) {
                processUserRequest(userInput);
            }

            pressEnterToContinue();
        }

        scanner.close();
    }

    private static void printWelcomeBanner() {
        System.out.println("\n" + repeatString("=", 60));
        System.out.println("🤖 Welcome to the AI Study Guide Assistant! 📚");
        System.out.println(repeatString("=", 60) + "\n");
    }

    private static void showThinkingAnimation() {
        System.out.print("\n🤔 Thinking");
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(400);
                System.out.print(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("\n");
    }

    private static void printResponse(String response) {
        System.out.println(repeatString("=", 60));
        System.out.println("📚 AI Response:");
        System.out.println(repeatString("=", 60) + "\n");
        System.out.println(response + "\n");
    }

    private static void printGoodbyeBanner() {
        System.out.println("\n" + repeatString("=", 60));
        System.out.println("👋 Thank you for using the AI Study Guide Assistant!");
        System.out.println("Keep learning and growing! 🌱");
        System.out.println(repeatString("=", 60) + "\n");
    }

    private static void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // ==============================================
    // SECTION 3: Caching & Similarity Analysis
    // ==============================================

    private static void processUserRequest(String userInput) {
        showThinkingAnimation();

        Map<String, String> savedResponses = loadSavedResponses();
        System.out.println("📂 Loaded " + savedResponses.size() + " saved responses from cache.");

        String similarQuestion = findSimilarQuestionInCache(userInput, savedResponses);
        String response;

        if (similarQuestion != null) {
            System.out.println("✅ Found a similar question in the cache.");
            response = savedResponses.get(similarQuestion);
        } else {
            System.out.println("🔍 No similar question found in cache. Querying AI...");
            response = getAIResponse(userInput);
            if (response != null && !response.isEmpty()) {
                saveResponseToFile(userInput, response);
            }
        }

        if (response != null && !response.isEmpty()) {
            printResponse(response);
        } else {
            System.out.println("❌ Sorry, I couldn't get a response right now. Please try again.");
        }
    }

    private static Map<String, String> loadSavedResponses() {
        Map<String, String> responses = new LinkedHashMap<>();
        File file = new File(CACHE_FILE);

        if (!file.exists()) {
            return responses;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String question = null;
            StringBuilder answer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Q: ")) {
                    if (question != null) {
                        responses.put(question, answer.toString().trim());
                    }
                    question = line.substring(3).trim();
                    answer.setLength(0);
                } else if (line.startsWith("A: ")) {
                    answer.append(line.substring(3).trim()).append("\n");
                } else if (!line.trim().isEmpty()) {
                    answer.append(line).append("\n");
                }
            }
            if (question != null) {
                responses.put(question, answer.toString().trim());
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading cache file: " + e.getMessage());
        }
        return responses;
    }

    private static void saveResponseToFile(String question, String answer) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CACHE_FILE, true))) {
            writer.write("Q: " + question);
            writer.newLine();
            writer.write("A: " + answer);
            writer.newLine();
            writer.newLine();
            System.out.println("✅ Response saved to cache!");
        } catch (IOException e) {
            System.out.println("❌ Error saving response to cache: " + e.getMessage());
        }
    }

    private static String findSimilarQuestionInCache(String newQuestion, Map<String, String> savedResponses) {
        if (savedResponses.isEmpty()) return null;

        String bestMatch = null;
        double highestScore = 0.0;

        for (String savedQuestion : savedResponses.keySet()) {
            double score = calculateSimilarityScore(newQuestion, savedQuestion);
            if (score > highestScore) {
                highestScore = score;
                bestMatch = savedQuestion;
            }
        }

        if (bestMatch != null && highestScore >= SIMILARITY_THRESHOLD) {
            System.out.printf("✅ Found a match with a similarity score of %.2f%%\n", highestScore * 100);
            System.out.println("   → New Question: \"" + newQuestion + "\"");
            System.out.println("   → Cached Match: \"" + bestMatch + "\"");
            return bestMatch;
        }

        System.out.println("❌ No question in cache met the similarity threshold.");
        return null;
    }

    /**
     * Calculates similarity based on semantic intent and keyword overlap (including bigrams).
     * @return A score between 0.0 and 1.0.
     */
    private static double calculateSimilarityScore(String q1, String q2) {
        String q1Normalized = q1.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
        String q2Normalized = q2.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();

        // 1. Semantic Intent & Topic Similarity (High Importance)
        String[] intentAndTopic1 = identifyQuestionPattern(q1Normalized, GENERAL_STOP_WORDS);
        String[] intentAndTopic2 = identifyQuestionPattern(q2Normalized, GENERAL_STOP_WORDS);

        double intentSimilarity = 0.0;
        if (intentAndTopic1[0].equals(intentAndTopic2[0])) { // Pattern types match
            intentSimilarity = 0.5;
            if (intentAndTopic1[1] != null && intentAndTopic1[1].equals(intentAndTopic2[1])) { // Core topics also match
                intentSimilarity = 1.0; // Very strong signal if both intent and topic match
            }
        }

        // 2. Keyword Overlap (Unigrams & Bigrams)
        List<String> words1 = getImportantWords(q1Normalized, GENERAL_STOP_WORDS);
        List<String> words2 = getImportantWords(q2Normalized, GENERAL_STOP_WORDS);

        double unigramSimilarity = calculateJaccardIndex(new HashSet<>(words1), new HashSet<>(words2));

        Set<String> bigrams1 = getNgrams(words1, 2);
        Set<String> bigrams2 = getNgrams(words2, 2);
        double bigramSimilarity = calculateJaccardIndex(bigrams1, bigrams2);

        // Weighted average of unigram and bigram similarity
        double keywordSimilarity = (unigramSimilarity * 0.7) + (bigramSimilarity * 0.3);

        // Final combined score with more weight on intent matching
        return (intentSimilarity * 0.6) + (keywordSimilarity * 0.4);
    }

    private static double calculateJaccardIndex(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Identifies the question's semantic pattern and extracts the core topic.
     * @return A String array: [0] = pattern type (e.g., "WHAT_IS"), [1] = core topic.
     */
    private static String[] identifyQuestionPattern(String question, Set<String> stopWords) {
        String[][] patterns = {
                { "WHAT_IS", String.join("|", WHAT_IS_PATTERNS) },
                { "HOW_TO", String.join("|", HOW_TO_PATTERNS) },
                { "WHY", String.join("|", WHY_PATTERNS) },
                { "COMPARISON", String.join("|", COMPARISON_PATTERNS) },
                { "EXAMPLES", String.join("|", EXAMPLE_PATTERNS) }
        };

        for (String[] patternInfo : patterns) {
            String type = patternInfo[0];
            String regex = "\\b(" + patternInfo[1] + ")\\b";
            if (question.matches(".*" + regex + ".*")) {
                String topicFragment = question.replaceAll(regex, "").trim();
                return new String[]{type, extractCoreTopic(topicFragment, stopWords)};
            }
        }

        return new String[]{"GENERAL", extractCoreTopic(question, stopWords)};
    }

    /**
     * Extracts the most important 1-3 stemmed words from a question fragment to define its topic.
     */
    private static String extractCoreTopic(String questionFragment, Set<String> stopWords) {
        List<String> words = getImportantWords(questionFragment, stopWords);
        if (words.isEmpty()) return null;

        StringBuilder topic = new StringBuilder();
        // Use the first few important words to represent the topic
        for (int i = 0; i < Math.min(words.size(), 3); i++) {
            topic.append(words.get(i)).append(" ");
        }
        return topic.toString().trim();
    }

    /**
     * Extracts important (non-stop, stemmed) words from a string.
     */
    private static List<String> getImportantWords(String text, Set<String> stopWords) {
        List<String> importantWords = new ArrayList<>();
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                importantWords.add(stem(word));
            }
        }
        return importantWords;
    }

    /**
     * Generates a set of n-grams (word sequences) from a list of words.
     * @param words The list of words.
     * @param n The size of the n-gram (e.g., 2 for bigrams).
     * @return A set of n-grams.
     */
    private static Set<String> getNgrams(List<String> words, int n) {
        Set<String> bigrams = new HashSet<>();
        if (words.size() < 2) return bigrams;
        for (int i = 0; i < words.size() - 1; i++) {
            String bigram = words.get(i) + " " + words.get(i + 1);
            bigrams.add(bigram);
        }
        return bigrams;
    }

    /**
     * A simple stemming algorithm to reduce words to their root form.
     */
    private static String stem(String word) {
        word = word.toLowerCase();
        if (word.endsWith("s") && !word.endsWith("ss")) {
            word = word.substring(0, word.length() - 1);
        }
        if (word.endsWith("ing")) {
            word = word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ed")) {
            word = word.substring(0, word.length() - 2);
        }
        return word;
    }

    // ==============================================
    // SECTION 4: AI API Integration
    // ==============================================

    private static String getAIResponse(String userInput) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String requestBody = createRequestBody(userInput);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("❌ API request failed. Status: " + response.statusCode());
                System.out.println("   Response: " + response.body());
                return null;
            }

            return extractContentFromResponse(response.body());

        } catch (Exception e) {
            System.out.println("❌ Error during AI API call: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String createRequestBody(String userInput) {
        String escapedInput = userInput.replace("\"", "\\\"").replace("\n", "\\n");
        String escapedSystemPrompt = SYSTEM_PROMPT.replace("\"", "\\\"").replace("\n", "\\n");

        return "{" +
                "\"model\": \"" + MODEL + "\"," +
                "\"messages\": [" +
                "{\"role\": \"system\", \"content\": \"" + escapedSystemPrompt + "\"}," +
                "{\"role\": \"user\", \"content\": \"" + escapedInput + "\"}" +
                "]" +
                "}";
    }

    private static String extractContentFromResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody); // parsing
            JSONArray choices = json.getJSONArray("choices"); // choices[]
            if (choices.length() == 0) {
                System.out.println("❌ No choices returned by the AI.");
                return null;
            }
    
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message.getString("content"); // extract content
    
            return content;
    
        } catch (Exception e) {
            System.out.println("❌ JSON parsing failed: " + e.getMessage());
            return null;
        }
    }
    // ==============================================
    // SECTION 5: Utility Methods
    // ==============================================

    private static String repeatString(String str, int times) {
        return str.repeat(times);
    }
}