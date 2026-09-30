# Java Multi-Client Chat Application

A TCP client-server chat application in Java. One server handles many clients at the same time, with each client running on its own thread.

## Features
- Multiple users chatting at the same time (one thread per client)
- Broadcast messages to everyone
- Private messages with `/msg <user> <text>`
- Join and leave notifications
- Unique usernames (case-insensitive) with validation
- `/users` shows who is online, `/help` shows commands, `/quit` leaves the chat
- Handles sudden disconnects without crashing the server

## Tech Used
Java, Sockets (TCP/IP), Multithreading, Java Collections (`ConcurrentHashMap`), Exception Handling

## How It Works
- `ChatServer` opens a `ServerSocket` and waits for clients. Each accepted connection gets its own `ClientHandler` thread.
- `ClientHandler` reads what its client types and either broadcasts it or runs a command.
- `ChatClient` uses one thread to print incoming messages and the main thread to send what you type.
- Online users are stored in a thread-safe `ConcurrentHashMap`.

## How to Run
Requirements: JDK 8 or higher.

1. Compile (from the project folder):
```
cd src
javac *.java
```
2. Start the server (Terminal 1):
```
java ChatServer
```
3. Start one client per terminal (Terminal 2, 3, ...):
```
java ChatClient
```
To use another computer on the same Wi-Fi: `java ChatClient <server-ip> 5000`
To use another port: `java ChatServer 6000` and `java ChatClient localhost 6000`

## Commands
| Command | What it does |
|---|---|
| `/msg <user> <text>` | Send a private message |
| `/users` | List online users |
| `/help` | Show commands |
| `/quit` | Leave the chat |

## Screenshots
_Add a screenshot of the server and 2-3 clients chatting here._

## Author
Lakshya Sahu - [LinkedIn](https://www.linkedin.com/in/lakshya-825-sahu) | [LeetCode](https://leetcode.com/u/Lakshya62/)
