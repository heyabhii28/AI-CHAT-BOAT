package com.example.chatbot;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

public class ChatbotServer {
  private static final int PORT = 8080;
  private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
    server.createContext("/api/chat", ChatbotServer::chat);
    server.createContext("/", ChatbotServer::staticFile);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Java AI Chatbot: http://localhost:" + PORT);
    System.out.println("Set OPENAI_API_KEY in this terminal before launching for AI responses.");
  }

  private static void chat(HttpExchange ex) throws IOException {
    if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.getResponseHeaders().set("Access-Control-Allow-Origin","*"); ex.getResponseHeaders().set("Access-Control-Allow-Methods","POST, OPTIONS"); ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type"); ex.sendResponseHeaders(204,-1); return; }
    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,405,error("Use POST.")); return; }
    String key = System.getenv("OPENAI_API_KEY");
    if (key == null || key.isBlank()) { sendJson(ex,503,error("AI is not configured. Set OPENAI_API_KEY in the same terminal, then restart the server.")); return; }
    try {
      String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      JsonObject input = JsonParser.parseString(body).getAsJsonObject();
      String message = input.has("message") ? input.get("message").getAsString().trim() : "";
      if (message.isBlank()) { sendJson(ex,400,error("Please type a message.")); return; }
      if (message.length() > 8000) { sendJson(ex,400,error("Message too long (maximum 8000 characters).")); return; }

      JsonObject payload = new JsonObject();
      payload.addProperty("model","gpt-4.1-mini");
      JsonArray messages = new JsonArray();
      JsonObject system = new JsonObject(); system.addProperty("role","system"); system.addProperty("content","You are DevMentor, a helpful AI assistant. Be friendly and clear. Explain programming questions step by step for beginners."); messages.add(system);
      JsonObject user = new JsonObject(); user.addProperty("role","user"); user.addProperty("content",message); messages.add(user);
      payload.add("messages",messages);

      HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://api.openai.com/v1/chat/completions"))
        .timeout(Duration.ofSeconds(60)).header("Authorization","Bearer "+key).header("Content-Type","application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
      HttpResponse<String> response = CLIENT.send(req,HttpResponse.BodyHandlers.ofString());
      JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
      if (response.statusCode()<200 || response.statusCode()>=300) {
        String msg = "AI provider returned HTTP " + response.statusCode();
        if (result.has("error") && result.getAsJsonObject("error").has("message")) msg = result.getAsJsonObject("error").get("message").getAsString();
        sendJson(ex,502,error(msg)); return;
      }
      String reply = result.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
      JsonObject out = new JsonObject(); out.addProperty("reply",reply); sendJson(ex,200,out);
    } catch (Exception e) { sendJson(ex,500,error("Could not get an AI reply. Check your API key, internet connection, and server terminal.")); }
  }

  private static void staticFile(HttpExchange ex) throws IOException {
    String path = ex.getRequestURI().getPath(); if (path.equals("/")) path="/index.html";
    if (path.contains("..") || path.contains("\\\\")) { sendText(ex,400,"Bad path"); return; }
    try (InputStream in=ChatbotServer.class.getResourceAsStream("/static"+path)) {
      if (in==null) { sendText(ex,404,"Not found"); return; }
      byte[] data=in.readAllBytes();
      String type=path.endsWith(".html")?"text/html; charset=utf-8":path.endsWith(".css")?"text/css; charset=utf-8":path.endsWith(".js")?"application/javascript; charset=utf-8":"application/octet-stream";
      ex.getResponseHeaders().set("Content-Type",type); ex.sendResponseHeaders(200,data.length);
      try(OutputStream out=ex.getResponseBody()){out.write(data);}
    }
  }
  private static JsonObject error(String s){JsonObject o=new JsonObject();o.addProperty("error",s);return o;}
  private static void sendJson(HttpExchange ex,int status,JsonObject obj)throws IOException{
    ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
    ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
    byte[] b=obj.toString().getBytes(StandardCharsets.UTF_8);ex.sendResponseHeaders(status,b.length);
    try(OutputStream out=ex.getResponseBody()){out.write(b);}
  }
  private static void sendText(HttpExchange ex,int status,String s)throws IOException{
    byte[] b=s.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","text/plain; charset=utf-8");ex.sendResponseHeaders(status,b.length);
    try(OutputStream out=ex.getResponseBody()){out.write(b);}
  }
}