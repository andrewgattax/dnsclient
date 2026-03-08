# Advanced Networking assignment #1 - Andrea Gatta

## Note to the Professor

AI Agents were used to generate verbose parts of the code, such as CLI argument parsing logic, documentation, and repetitive refactoring tasks. However, the overall code structure and design approach—including the model-based architecture with `DNSMessage`, `Header`, `Question`, `ResourceRecord`, and the `DNSSerializable` interface pattern—is entirely my original work. I retain full intellectual property ownership of the architectural design and implementation strategy.

---

## DNS Client

A Java-based DNS client that performs DNS queries over UDP/53 and displays the results in a format similar to the `dig` tool.

## Usage

```
java -jar dnsclient-1.0.jar [options] <query> [TYPE]
```

### Positional Arguments

| Argument | Description | Required |
|----------|-------------|----------|
| `query` | The DNS name to resolve (e.g. `google.com`) | Yes |
| `TYPE` | The DNS record type: `A`, `AAAA`, `MX`, `CNAME`, `NS`, `TXT` | No — default: `A` |

`TYPE` is positional (not a flag), and must come **after** `query` if specified.

### Options

| Short | Long | Argument | Default | Description |
|-------|------|----------|---------|-------------|
| `-s` | `--server` | `<server>` | `127.0.0.53` | IPv4 address of the DNS resolver |
| `-r` | `--retries` | `<retries>` | `3` | Max number of query attempts before giving up |
| `-t` | `--timeout` | `<timeout>` | `1` | Timeout in seconds to wait for a reply |
| `-h` | `--help` | — | — | Print usage and exit |

Options can appear **anywhere** before or after positional arguments.

### Examples

Query for A records (default):
```bash
java -jar dnsclient-1.0.jar google.com
```

Query for MX records:
```bash
java -jar dnsclient-1.0.jar google.com MX
```

Query with custom DNS resolver:
```bash
java -jar dnsclient-1.0.jar -s 8.8.8.8 example.com A
```

Query with custom timeout and retries:
```bash
java -jar dnsclient-1.0.jar -t 5 -r 5 example.com AAAA
```

### Output Format

The output follows the standard DNS response format:

```
QUERY: <n>, ANSWER: <n>, AUTHORITY: <n>, ADDITIONAL: <n>

QUESTION SECTION:
<name>. IN <TYPE>

ANSWER SECTION:
<name>. <TTL> IN <TYPE> <rdata>

AUTHORITY SECTION:
<name>. <TTL> IN <TYPE> <rdata>

ADDITIONAL SECTION:
<name>. <TTL> IN <TYPE> <rdata>
```

Only non-empty sections are displayed (QUESTION and ANSWER are always shown).

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success (response received and printed) |
| `1` | Invalid arguments or unknown record type |
| `2` | Network failure — all retries exhausted with no response |

---

## Project Structure

```
dnsclient/
├── src/main/java/it/andrewfly/dnsclient/
│   ├── Main.java                          # Entry point, CLI parsing, network communication
│   ├── DNSSerializable.java               # Interface for byte serialization
│   ├── model/
│   │   ├── DNSMessage.java                # Root DNS message model
│   │   ├── Header.java                    # DNS header section
│   │   ├── Question.java                  # DNS question section
│   │   ├── ResourceRecord.java            # DNS resource record (answer/authority/additional)
│   │   ├── Type.java                      # DNS record types enum (A, AAAA, MX, etc.)
│   │   ├── DNSClass.java                  # DNS class enum (IN, etc.)
│   │   └── Flags.java                     # DNS header flags
│   └── service/
│       └── UtilService.java               # Utility functions for name encoding/decoding
└── pom.xml                                # Maven build configuration
```

---

## How It Works

### Building the Query

The query is constructed following the DNS protocol model through the `DNSMessage` class:

1. **`DNSMessage.buildQuery(String query, Type queryType)`** creates a complete DNS query message:
   - Generates a random 16-bit transaction ID
   - Sets standard query flags (QR=0, RD=1 for recursion desired)
   - Creates a `Header` with `qdCount=1` (one question)
   - Creates a `Question` containing the domain name, query type, and IN (Internet) class
   - Returns a `DNSMessage` object with the question populated

2. **`toByteArray()`** serializes the `DNSMessage` to a wire-format byte array:
   - Each component (Header, Question, ResourceRecord) implements the serialization logic
   - Domain names are encoded using DNS name compression (labels prefixed by length)
   - All numeric fields are written in big-endian network byte order

### Parsing the Response

The response is parsed using the reverse process:

1. **`DNSMessage.fromByteArray(byte[] bytes)`** reconstructs the message from raw bytes:
   - Parses the fixed 12-byte `Header` first to get section counts
   - Iterates through each section (Questions, Answers, Authorities, Additionals)
   - Each section's count from the header determines how many records to parse
   - Maintains an offset to track position in the byte array

2. **Parsing methods** (`Header.fromByteArray`, `Question.fromByteArray`, `ResourceRecord.fromByteArray`):
   - Read fields sequentially from the byte array
   - Handle DNS name decompression (pointer references)
   - Parse type-specific RDATA based on the record type
   - Return fully populated model objects

### Network Communication

The `Main` class orchestrates the UDP communication:

1. Creates a `DatagramSocket` with the configured timeout
2. Sends the serialized query to the DNS resolver on port 53
3. Waits for a response (with retry logic on timeout)
4. Passes the received bytes to `DNSMessage.fromByteArray()` for parsing
5. Formats and prints the response according to the specification
