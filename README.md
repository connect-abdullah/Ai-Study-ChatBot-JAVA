# AI Study Guide Chatbot

## Overview

The **AI Study Guide Chatbot** is a Java-based command-line assistant designed to help students learn more effectively. It leverages advanced AI (via the OpenRouter API) to answer educational questions, break down complex topics, and provide clear, student-friendly explanations. The chatbot also features a smart caching system to avoid redundant API calls and improve response times for repeated or similar questions.

---

## Features

- **Conversational Q&A:** Ask any study-related question and receive a detailed, structured, and easy-to-understand answer.
- **Smart Caching:** Previously answered questions (and similar ones) are stored locally. If a similar question is asked again, the cached answer is used, saving time and API usage.
- **Semantic Similarity Matching:** Uses a combination of intent detection, keyword overlap, and n-gram analysis to find similar questions in the cache.
- **Student-Friendly Formatting:** Responses are formatted with clear headings, bullet points, numbered steps, analogies, and code blocks (when relevant).
- **Encouraging Tone:** The chatbot is designed to be friendly, supportive, and approachable for learners of all ages.
- **Offline Recall:** Cached answers are available even if the AI API is temporarily unreachable.

---

## How It Works

1. **User Interaction:**
   - The user runs the chatbot in a terminal and is greeted with a welcome banner.
   - The user types a question (or `exit` to quit).
   - The chatbot processes the input and displays a "thinking" animation.

2. **Cache Lookup:**
   - The chatbot loads all previously saved Q&A pairs from `responses_cache.txt`.
   - It checks for a similar question using a custom similarity algorithm (semantic intent + keyword overlap).
   - If a match above the similarity threshold is found, the cached answer is shown.

3. **AI Query (if needed):**
   - If no similar question is found, the chatbot sends the user's question to the OpenRouter AI API (using the DeepSeek model).
   - The AI's response is parsed and displayed to the user.
   - The new Q&A pair is saved to the cache for future use.

4. **Repeat:**
   - The user can continue asking questions or exit the program.

---

## File Structure

```
pf project/
│
├── src/
│   ├── StudyChatbot.java         # Main Java source file
│   ├── StudyChatbot.class        # Compiled class file
│   └── responses_cache.txt       # (Sometimes also at root) Q&A cache file
│
├── lib/
│   └── json-20231013.jar         # JSON parsing library (org.json)
│
├── responses_cache.txt           # Main Q&A cache file (used by default)
├── pf project.iml                # IntelliJ IDEA project file
├── .gitignore
└── ... (other IDE/project files)
```

---

## Dependencies

- **Java 11+** (for HttpClient and modern language features)
- **org.json** (provided as `lib/json-20231013.jar`) for parsing API responses

---

## Setup & Usage

### 1. **Clone or Download the Project**

```sh
git clone <repo-url>
cd "pf project"
```

### 2. **Compile the Java Source**

```sh
javac -cp "lib/json-20231013.jar" -d src src/StudyChatbot.java
```

### 3. **Run the Chatbot**

```sh
cd src
java -cp ".:../lib/json-20231013.jar" StudyChatbot
```

> **Note:** On Windows, replace `:` with `;` in the classpath.

### 4. **Ask Questions!**

- Type your study question and press Enter.
- To exit, type `exit` and press Enter.

---

## Example Interaction

```
🤖 Welcome to the AI Study Guide Assistant! 📚

📝 Ask a question, or type 'exit' to quit.
➜ What is photosynthesis?

🤔 Thinking...

📚 AI Response:
===============================
Photosynthesis is the process by which plants...
...
```

---

## Caching System

- All Q&A pairs are stored in `responses_cache.txt` in the format:
  ```
  Q: <question>
  A: <answer>
  ```
- When a new question is asked, the chatbot checks for similar questions using:
  - Semantic intent (e.g., "What is...", "How to...", "Why...")
  - Core topic extraction (ignoring stop words)
  - Keyword and bigram overlap (Jaccard index)
- If a match is found (similarity ≥ 0.72), the cached answer is used.

---

## Customization

- **API Key:** The OpenRouter API key is hardcoded in the source for demonstration. For security, consider loading it from an environment variable or config file.
- **Model & Prompt:** You can change the AI model or system prompt in `StudyChatbot.java` to adjust the chatbot's behavior or tone.
- **Cache File:** By default, the cache is `responses_cache.txt` in the project root.

---

## Use Cases

- **Self-Study:** Get instant, clear explanations for any topic.
- **Homework Help:** Break down complex problems into simple steps.
- **Revision:** Ask for summaries, key points, or examples.
- **Tutoring:** Use as a digital assistant for students.

---

## Limitations & Notes

- **Internet Required:** For new questions, an internet connection is needed to query the AI API.
- **Cache Growth:** The cache file can grow large over time; you may want to clear or archive it periodically.
- **API Limits:** Excessive use may hit API rate limits or quotas, depending on your OpenRouter plan.
- **Security:** The API key should be kept private in production environments.

---

## Credits

- **AI Responses:** Powered by OpenRouter (DeepSeek model)
