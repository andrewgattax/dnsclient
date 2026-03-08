# CLI Guidelines — `dnslookup` DNS Client

## 1. Command Syntax

```
./dnslookup [options] <query> [TYPE]
```

### Positional Arguments

| Argument | Description | Required |
|----------|-------------|----------|
| `query` | The DNS name to resolve (e.g. `google.com`) | Yes |
| `TYPE` | The DNS record type: `A`, `AAAA`, `MX`, `CNAME`, `NS`, `TXT` | No — default: `A` |

`TYPE` is positional (not a flag), and must come **after** `query` if specified.

---

## 2. Options / Flags

| Short | Long | Argument | Default | Description |
|-------|------|----------|---------|-------------|
| `-s` | `--server` | `<server>` | `127.0.0.53` | IPv4 address of the DNS resolver |
| `-r` | `--retries` | `<retries>` | `3` | Max number of query attempts before giving up |
| `-t` | `--timeout` | `<timeout>` | `1` | Timeout in seconds to wait for a reply |
| `-h` | `--help` | — | — | Print usage and exit |

Options can appear **anywhere** before or after positional arguments.

---

## 3. Argument Parsing Rules

- Use `getopt_long` (from `<getopt.h>`) to handle both short and long options.
- After `getopt_long` processing, the remaining `argv` entries are the positional arguments: `argv[optind]` = `query`, `argv[optind+1]` = `TYPE` (if present).
- `TYPE` matching must be **case-insensitive** (accept `a`, `A`, `aaaa`, `AAAA`, etc.). Internally normalise to uppercase.
- If an unrecognised `TYPE` is passed, print an error to `stderr` and exit with a non-zero code.
- If `query` is missing, print usage to `stderr` and exit with a non-zero code.
- `-h` / `--help` must print the full usage block (see §4) to `stdout` and exit with code `0`.

---

## 4. Help / Usage Output (`-h`, `--help`)

```
Usage: ./dnslookup [options] <query> [TYPE]

Arguments:
  query             The DNS query to solve
  TYPE              The DNS record type for the query. Supported values are
                    A, AAAA, MX, CNAME, NS, TXT (default: A)

Options:
  -s <server>, --server <server>
                    The IPv4 address of the DNS resolver (default: 127.0.0.53)
  -r <retries>, --retries <retries>
                    The maximum number of retries before declaring failure (default: 3)
  -t <timeout>, --timeout <timeout>
                    The timeout for receiving the DNS reply in seconds (default: 1)
  -h, --help
                    Display this help and exit
```

---

## 5. Standard Output Format

All output goes to **`stdout`**. Errors and warnings go to **`stderr`**.

### 5.1 Header Line

Always printed first, on a single line:

```
QUERY: <n>, ANSWER: <n>, AUTHORITY: <n>, ADDITIONAL: <n>
```

The four counts correspond to the respective section counts in the DNS reply.

### 5.2 Section Blocks

Print only **non-empty** sections, in this fixed order:

1. `QUESTION SECTION:`
2. `ANSWER SECTION:`
3. `AUTHORITY SECTION:` *(if count > 0)*
4. `ADDITIONAL SECTION:` *(if count > 0)*

Each section starts with its label on its own line, followed by one record per line.

### 5.3 Record Format per Section

**QUESTION SECTION** — one line per question:
```
<name>. IN <TYPE>
```

**ANSWER / AUTHORITY / ADDITIONAL SECTIONS** — one line per resource record:
```
<name>. <TTL> IN <TYPE> <rdata>
```

`<rdata>` format per record type:

| TYPE | `<rdata>` format | Example |
|------|-----------------|---------|
| `A` | IPv4 address | `142.251.140.110` |
| `AAAA` | IPv6 address | `2a00:1450:4002:411::200e` |
| `MX` | `<priority> <exchange>` | `10 smtp.google.com` |
| `NS` | nameserver hostname | `ns1.google.com` |
| `CNAME` | canonical name | `umass.edu.cdn.cloudflare.net` |
| `TXT` | raw text string (no extra quotes) | `v=spf1 include:_spf.google.com ~all` |

### 5.4 Name Formatting

- All domain names in the output must be **fully qualified**, ending with a trailing dot (`.`).
  - e.g. `google.com.`, `smtp.google.com.`, `ns1.google.com.`

### 5.5 Concrete Output Examples

**A query:**
```
QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 0
QUESTION SECTION:
google.com. IN A
ANSWER SECTION:
google.com. 179 IN A 142.251.140.110
```

**MX query (with ADDITIONAL):**
```
QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 9
QUESTION SECTION:
google.com. IN MX
ANSWER SECTION:
google.com. 275 IN MX 10 smtp.google.com
ADDITIONAL SECTION:
smtp.google.com. 119 IN A 64.233.166.26
...
```

**NS query (with ADDITIONAL):**
```
QUERY: 1, ANSWER: 4, AUTHORITY: 0, ADDITIONAL: 8
QUESTION SECTION:
google.com. IN NS
ANSWER SECTION:
google.com. 171620 IN NS ns1.google.com
google.com. 171620 IN NS ns2.google.com
google.com. 171620 IN NS ns3.google.com
google.com. 171620 IN NS ns4.google.com
ADDITIONAL SECTION:
ns1.google.com. 171691 IN A 216.239.32.10
...
```

**CNAME query:**
```
QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 0
QUESTION SECTION:
www.umass.edu. IN CNAME
ANSWER SECTION:
www.umass.edu. 3572 IN CNAME umass.edu.cdn.cloudflare.net
```

---

## 6. Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success (response received and printed) |
| `1` | Invalid arguments or unknown record type |
| `2` | Network failure — all retries exhausted with no response |
