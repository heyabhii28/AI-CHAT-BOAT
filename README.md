# DevMentor AI Chatbot — Java

Requirements: JDK 17+, Maven, internet, and an OpenAI API key with API access/billing enabled.

## Run on Windows / VS Code
1. Extract the ZIP and open the folder containing `pom.xml`.
2. Open VS Code Terminal and check:
   ```powershell
   java -version
   mvn -version
   ```
3. Set your API key in that same PowerShell terminal (replace the placeholder with your real key):
   ```powershell
   $env:OPENAI_API_KEY="YOUR_API_KEY_HERE"
   ```
   Keep the key private. Do not put it in `app.js`, commit it, or share screenshots showing it.
4. Build:
   ```powershell
   mvn clean package
   ```
5. Start the server:
   ```powershell
   mvn exec:java -Dexec.mainClass="com.example.chatbot.ChatbotServer"
   ```
6. Open http://localhost:8080 in your browser. Stop server with Ctrl+C.

The AI API can cost money; check the current API pricing and usage limits first.

## Common issues
- `mvn is not recognized`: install Maven and reopen VS Code.
- `AI is not configured`: set the environment variable in the same terminal and restart.
- HTTP 401: API key invalid/revoked.
- HTTP 429: API quota, billing, or rate limit.
- `Address already in use`: change PORT in ChatbotServer.java or stop the process using 8080.
